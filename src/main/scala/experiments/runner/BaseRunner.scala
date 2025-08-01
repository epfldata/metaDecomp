package experiments.runner

import decompositions.PlanNode
import experiments.Config.*
import sql.Attribute

import java.io.File
import java.nio.file.{Files, Paths}
import java.sql.{Connection, DriverManager, Statement}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future, TimeoutException}
import scala.io.Source
import scala.sys.process.*

trait BaseRunner {
	val conn: Connection = DriverManager.getConnection(dataSource)

//	private val setMemoryStmt: Statement = conn.createStatement()
//	setMemoryStmt.execute("SET memory_limit = '40GB';")
//	setMemoryStmt.execute("SET temp_directory = '';") // Disable spilling to disk, just let it crash
//	setMemoryStmt.close()

	deleteTmpFiles()

	def deleteTmpFiles(): Unit = {
		try {
			Seq("bash", "-c", s"rm -rf $dbFilePath.tmp/*").!
		} catch {
			case _: Throwable => ()
		}
	}

	def runPlan(plan: PlanNode)(implicit sqlIR: sql.IR): Long = {
		val (viewSqls, finalSql, groupBy) = plan.generateSqlWithViews()
		runQueryWithViewCreation(viewSqls, finalSql + "\n" + groupBy + ";")
	}

	def runQuery(query: String): Long = runQueryWithViewCreation("", query + ";")

	def runQueryWithViewCreation(viewSqls: String, finalSql: String): Long = {
		val executionTime = (for (i <- 0 until repeatTimes) yield {

			val viewsStmt = conn.createStatement()
			val stmt = conn.createStatement()
			stmt.setQueryTimeout(timeout.toSeconds.toInt)

			try {
				if (viewSqls.nonEmpty) {
					viewsStmt.execute(viewSqls)
					viewsStmt.close()
				}

				val queryFuture = Future { stmt.execute(finalSql) }

				val executionStartTime = System.nanoTime()
				Await.result(queryFuture, timeout)
				val executionEndTime = System.nanoTime()
				val executionTime = (executionEndTime - executionStartTime) / 1000 // microseconds
				println(s"Execution run $i: $executionTime us")
				stmt.close()
				executionTime
			} catch {
				case e: TimeoutException =>
					stmt.cancel()
					println("Query timed out and was terminated.")
					if (i == 0) {
						return timeout.toMicros // Indicating failure on the first run
					} else {
						timeout.toMicros // Indicating failure
					}
				case _ =>
					println("Query exceeded memory limit or crashed and was terminated.")
					if (i == 0) {
						return timeout.toMicros // Indicating failure on the first run
					} else {
						timeout.toMicros // Indicating failure
					}
			}
		}).sorted.apply(repeatTimes / 2) // Take the median of 5 runs

		executionTime
	}
}
