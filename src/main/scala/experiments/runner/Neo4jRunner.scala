package experiments.runner

import experiments.Config.*
import experiments.readInOneLine
import experiments.getTimestamp
import experiments.median

import java.io.File
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.time.Duration as JDuration
import java.util.concurrent.TimeUnit
import scala.collection.mutable
import scala.concurrent.{Await, Future, TimeoutException}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*

import org.neo4j.driver.{AuthTokens, Driver, GraphDatabase, Session, SessionConfig, Result, TransactionConfig}
import org.neo4j.driver.exceptions.{Neo4jException, TransactionTerminatedException}
import org.neo4j.driver.summary.ResultSummary

object Neo4jRunner {
	var driver: Driver = null

	def connect(): Unit = {
		driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.none())
		driver.verifyConnectivity()
	}

	def close(): Unit = {
		if (driver != null) {
			try { driver.closeAsync() } catch { case _: Throwable => }
			driver = null
		}
	}

	def killRunningQueries(): Unit = {
		var tempDriver: Driver = null
		try {
			tempDriver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.none())
			val session = tempDriver.session(SessionConfig.forDatabase("system"))
			val shortTxConfig = TransactionConfig.builder()
				.withTimeout(JDuration.ofSeconds(3))
				.build()
			try {
				val txIds = mutable.ListBuffer[String]()
				try {
					val res = session.run(
						"""SHOW TRANSACTIONS YIELD transactionId, currentQuery
						  |WHERE currentQuery IS NOT NULL
						  |  AND NOT currentQuery STARTS WITH "SHOW TRANSACTIONS"
						  |  AND NOT currentQuery STARTS WITH "TERMINATE"
						  |RETURN transactionId""".stripMargin,
						shortTxConfig
					)
					while (res.hasNext) {
						txIds.addOne(res.next().get("transactionId").asString())
					}
					res.consume()
				} catch {
					case _: Throwable =>
				}

				for (txId <- txIds) {
					try {
						val termRes = session.run(s"TERMINATE TRANSACTION '$txId'", shortTxConfig)
						termRes.consume()
						println(s"Terminated active Neo4j transaction: $txId")
					} catch {
						case _: Throwable =>
					}
				}
			} finally {
				try { session.close() } catch { case _: Throwable => }
			}
		} catch {
			case _: Throwable =>
		} finally {
			if (tempDriver != null) {
				try { tempDriver.closeAsync() } catch { case _: Throwable => }
			}
		}
	}

	def resetDriver(): Unit = {
		try {
			val f = Future { killRunningQueries() }
			scala.util.Try(Await.result(f, 2.seconds))
		} catch {
			case _: Throwable =>
		}
		close()
		connect()
	}

	def main(args: Array[String]): Unit = {
		sys.addShutdownHook {
			println("\n[Neo4jRunner] Shutdown signal received. Terminating running queries in Neo4j...")
			killRunningQueries()
			close()
		}

		val targetBenchmarks = if (args.nonEmpty) List(args(0)) else List("subgraph-matching")

		for (benchmark <- targetBenchmarks) {
			connect()

			val resultsPath = Paths.get(resultsDir, s"neo4j-$benchmark-$getTimestamp.csv")
			Files.createDirectories(Paths.get(resultsDir))

			Files.write(
				resultsPath,
				"query,opt_time,exec_time,total_time\n".getBytes,
				StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
			)

			val queryFiles = cypherFilesInBenchmark(benchmark)
				.filter(f => if (args.length >= 2) f.getName.matches(args(1)) else true)
				.filter(f => {
					Files.exists(Paths.get(s"$benchmarksPath/$benchmark/cardinalities/${f.getName.stripSuffix(".cypher")}.csv"))
				})

			val txConfig = TransactionConfig.builder()
				.withTimeout(JDuration.ofMillis(timeout.toMillis))
				.build()

			for (cypherFile <- queryFiles) {
				val queryName = cypherFile.getName.stripSuffix(".cypher")
				println("\n-----------------------------------")
				println(s"${cypherFile.getName}")

				val query = readInOneLine(cypherFile).stripSuffix(";")

				// Measure Optimization Time and Total End-to-End Evaluation Time in a single run
				var failedOnRun0 = false
				case class RunMetrics(optTime: Double, execTime: Double, totalTime: Double)
				val runResults = mutable.ListBuffer[RunMetrics]()

				for (i <- 0 until repeatTimes if !failedOnRun0) {
					var sessionNeedsClose = true
					val session = driver.session()
					val optFile = Paths.get(neo4jOptTimeLog)
					try {
						val future = Future {
							Files.deleteIfExists(optFile)
							session.run("CALL db.clearQueryCaches()").consume()
							val t0 = System.nanoTime()
							val res = session.run(query, txConfig)
							var count = 0L
							while (res.hasNext) {
								res.next()
								count += 1
							}
							val summary = res.consume()
							val t1 = System.nanoTime()
							val elapsedTotalUs = (t1 - t0) / 1000.0

							val elapsedOptUs = if (Files.exists(optFile)) {
								val lines = Files.readString(optFile).trim.split("\n").filter(_.nonEmpty).map(_.trim.toDouble)
								if (lines.nonEmpty) lines.sum else 0.0
							} else {
								0.0
							}
							(elapsedOptUs, elapsedTotalUs, count)
						}
						println(s"Run $i started $getTimestamp:")
						val (optRunUs, totalRunUs, rowCount) = Await.result(future, timeout)
						val execRunUs = math.max(0.0, totalRunUs - optRunUs)
						println(f"Run $i: opt: $optRunUs%.1f us, exec: $execRunUs%.1f us, total: $totalRunUs%.1f us (rows: $rowCount)")
						runResults.addOne(RunMetrics(optRunUs, execRunUs, totalRunUs))
					} catch {
						case _: TimeoutException =>
							println("Query timed out (client-side Await timeout reached).")
							sessionNeedsClose = false
							resetDriver()
							if (i == 0) failedOnRun0 = true
							val optRunUs = if (Files.exists(optFile)) {
								val lines = Files.readString(optFile).trim.split("\n").filter(_.nonEmpty).map(_.trim.toDouble)
								if (lines.nonEmpty) lines.sum else timeout.toMicros.toDouble
							} else {
								timeout.toMicros.toDouble
							}
							val tTimeout = timeout.toMicros.toDouble
							runResults.addOne(RunMetrics(optRunUs, tTimeout, tTimeout))
						case e: TransactionTerminatedException =>
							println(s"Query terminated by Neo4j server (e.g. memory limit or server timeout): ${e.getMessage}")
							sessionNeedsClose = false
							resetDriver()
							if (i == 0) failedOnRun0 = true
							val optRunUs = if (Files.exists(optFile)) {
								val lines = Files.readString(optFile).trim.split("\n").filter(_.nonEmpty).map(_.trim.toDouble)
								if (lines.nonEmpty) lines.sum else timeout.toMicros.toDouble
							} else {
								timeout.toMicros.toDouble
							}
							val tTimeout = timeout.toMicros.toDouble
							runResults.addOne(RunMetrics(optRunUs, tTimeout, tTimeout))
						case e: Neo4jException =>
							println(s"Neo4j server error (${e.code()}): ${e.getMessage}")
							sessionNeedsClose = false
							resetDriver()
							if (i == 0) failedOnRun0 = true
							val optRunUs = if (Files.exists(optFile)) {
								val lines = Files.readString(optFile).trim.split("\n").filter(_.nonEmpty).map(_.trim.toDouble)
								if (lines.nonEmpty) lines.sum else timeout.toMicros.toDouble
							} else {
								timeout.toMicros.toDouble
							}
							val tTimeout = timeout.toMicros.toDouble
							runResults.addOne(RunMetrics(optRunUs, tTimeout, tTimeout))
						case e: Throwable =>
							println(s"Query crashed: ${e.getMessage}")
							sessionNeedsClose = false
							resetDriver()
							if (i == 0) failedOnRun0 = true
							val optRunUs = if (Files.exists(optFile)) {
								val lines = Files.readString(optFile).trim.split("\n").filter(_.nonEmpty).map(_.trim.toDouble)
								if (lines.nonEmpty) lines.sum else timeout.toMicros.toDouble
							} else {
								timeout.toMicros.toDouble
							}
							val tTimeout = timeout.toMicros.toDouble
							runResults.addOne(RunMetrics(optRunUs, tTimeout, tTimeout))
					} finally {
						if (sessionNeedsClose) {
							try { session.close() } catch { case _: Throwable => }
						}
					}
				}

				val medianRun = if (failedOnRun0) {
					runResults.headOption.getOrElse {
						val tTimeout = timeout.toMicros.toDouble
						RunMetrics(tTimeout, tTimeout, tTimeout)
					}
				} else {
					runResults.sortBy(_.totalTime).apply(runResults.size / 2)
				}

				val optTime = medianRun.optTime
				val execTime = medianRun.execTime
				val totalTime = medianRun.totalTime

				println(s"Optimization time: $optTime us")
				println(s"Execution time: $execTime us")
				println(s"Total time: $totalTime us")

				Files.write(
					resultsPath,
					s"$queryName,$optTime,$execTime,$totalTime\n".getBytes,
					StandardOpenOption.APPEND
				)
			}

			close()
		}
	}
}



