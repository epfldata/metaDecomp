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
import java.util.Properties
import experiments.getTimestamp
import java.sql.SQLTimeoutException

trait BaseRunner {
	var conn: Connection = null

	def connect(benchmark: String): Unit = {
		val readOnlyProperty = new Properties();
		readOnlyProperty.setProperty("duckdb.read_only", "true");
		conn = DriverManager.getConnection(dataSource(benchmark), readOnlyProperty)
		val stmt: Statement = conn.createStatement()
		stmt.execute("SET memory_limit = '40GB';")
		stmt.execute("SET temp_directory = '';") // Disable spilling to disk, just let it crash
		// stmt.execute("SET max_temp_directory_size = '10GB';")
		stmt.close()
		deleteTmpFiles()
	}

	// For metaDecomp and DPconv, the join order optimizer will not affect the order of join operations, since we use a sequence of temporary views.
	// If does however decide, e.g., the inner and outer relations (left/right-hand side) of each join step in the physical plan.
	// So it is not unfair to enable it when measuring the performance for the two non-DuckDB optimizers.
	// But we notice it is very problematic for large queries, so we use a simple heuristic to decide whether to disable it.
	// This is consistent between metaDecomp and DPconv. For DuckDB, we always keep all optimizers enabled.
	def toggleOptimizers(condition: Boolean): Unit = {
		val stmt: Statement = conn.createStatement()
		stmt.execute(s"SET disabled_optimizers = '${if (condition) "" else "join_order,statistics_propagation"}';")
		stmt.close()
	}

	def deleteTmpFiles(): Unit = {
		try {
			benchmarks.foreach(benchmark => Seq("bash", "-c", s"rm -rf ${dbFilePath(benchmark)}.tmp/*").!)
		} catch {
			case e: Throwable => e.printStackTrace()
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
				if (viewSqls.stripSuffix(";").nonEmpty) {
					viewsStmt.execute(viewSqls)
					viewsStmt.close()
				}

				val queryFuture = Future { stmt.execute(finalSql) }

				val executionStartTime = System.nanoTime()
				println(s"Execution run $i started $getTimestamp:")
				Await.result(queryFuture, timeout)
				val executionEndTime = System.nanoTime()
				val executionTime = (executionEndTime - executionStartTime) / 1000 // microseconds
				println(s"Execution run $i: $executionTime us")
				stmt.close()
				executionTime
			} catch {
				case _: TimeoutException | _: SQLTimeoutException =>
					println("Query timed out, terminating...")
					stmt.cancel()
					deleteTmpFiles()
					println("Query timed out and was terminated.")
					if (i == 0) {
						return timeout.toMicros // Indicating failure on the first run
					} else {
						timeout.toMicros // Indicating failure
					}
				case e: Throwable =>
					e.printStackTrace()
					deleteTmpFiles()
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
