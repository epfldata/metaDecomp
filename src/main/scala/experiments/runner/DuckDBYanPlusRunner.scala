package experiments.runner

import experiments.Config.*
import experiments.readInOneLine

import java.nio.file.{Files, Paths, StandardOpenOption}
import experiments.getTimestamp
import scala.sys.process._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable
import experiments.median
import scala.concurrent.TimeoutException
import java.sql.SQLTimeoutException

object DuckDBYanPlusRunner extends BaseRunner {
	def main(args: Array[String]): Unit = {
		for (benchmark <- benchmarks) {
			connect(benchmark)

			val resultsPath = Paths.get(resultsDir, s"yanplus-$benchmark-$getTimestamp.csv")

			Files.write(
				resultsPath,
				"query,opt_time,exec_time,total_time\n".getBytes,
				StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
			)


			sqlFilesInBenchmark(benchmark).filter(f => {
				Files.exists(Paths.get(s"$benchmarksPath/$benchmark/cardinalities/${f.getName.stripSuffix(".sql")}.csv"))
			}).foreach(sqlFile =>
				println("\n-----------------------------------")
				println(s"${sqlFile.getName}")

				val queryWithGroupBy = readInOneLine(sqlFile)
				val query = queryWithGroupBy
				// .substring(0, queryWithGroupBy.replace("group by", "GROUP BY").indexOf("GROUP BY "))
				.replace("select", "SELECT")
				.replace("SELECT ", "SELECT SUM(1), ")
				// .replaceFirst("SELECT\\s+([a-zA-Z0-9_]+\\.[a-zA-Z0-9_]+)", "SELECT MIN($1), $1")

				val explainCommand = Seq("./DuckDBYanPlus/build/release/duckdb", "-readonly", dbFilePath(benchmark), "-c", "EXPLAIN " + query)

				println(explainCommand.mkString(" "))

				val optTime = (for (i <- 0 until repeatTimes) yield {
					val output = mutable.ListBuffer[String]()
					val error = mutable.ListBuffer[String]()
					val process = Process(explainCommand).run(ProcessLogger(fout = output.addOne, ferr = error.addOne))
					val future = Future(process.exitValue())
					val exitCode = Await.result(future, timeout)
					// println(output.mkString("\n"))
					(output ++ error).find(line => line.contains("All Yan+ optimization time: ")).get.stripPrefix("All Yan+ optimization time: ").stripSuffix(" microseconds").toDouble
				}).median()

				println(s"Optimization time: $optTime us")

				val totalTime = runQueryInCommandLine(query, dbFilePath(benchmark))

				val execTime = if totalTime < timeout.toMicros - 1e-5 then totalTime - optTime else timeout.toMicros
				println(s"Execution time: $execTime us")
				println(s"Total time: $totalTime us")


				Files.write(resultsPath, s"${sqlFile.getName.stripSuffix(".sql")},$optTime,$execTime,$totalTime\n".getBytes, StandardOpenOption.APPEND)
			)

			conn.close()
		}

	}

	def runQueryInCommandLine(sql: String, dbFilePath: String): Double = {
		val executionTime = (for (i <- 0 until repeatTimes) yield {
			val execCommand = Seq("./DuckDBYanPlus/build/release/duckdb", "-readonly", dbFilePath, "-c", "SET memory_limit = '40GB'", "-c", "SET temp_directory = ''", "-c", ".timer on", "-c", sql)

			val output = mutable.ListBuffer[String]()
			val error = mutable.ListBuffer[String]()
			val process = Process(execCommand).run(ProcessLogger(fout = output.addOne, ferr = error.addOne))
			val future = Future(process.exitValue())
			try {
				val exitCode = Await.result(future, timeout)
				if (exitCode != 0) {
					throw new Exception(s"Query failed with exit code $exitCode")
				}
			} catch {
				case _: TimeoutException | _: SQLTimeoutException =>
					println("Query timed out, terminating...")
					process.destroy()
					println("Query timed out and was terminated.")
					if (i == 0) {
						return timeout.toMicros // Indicating failure on the first run
					} else {
						timeout.toMicros // Indicating failure
					}
				case e: Throwable =>
					e.printStackTrace()
					if (i == 0) {
						return timeout.toMicros // Indicating failure on the first run
					} else {
						timeout.toMicros // Indicating failure
					}
			}

			val intermediateStr = (output ++ error).find(line => line.contains("Run Time (s): real ")).get.stripPrefix("Run Time (s): real ")
			intermediateStr.substring(0, intermediateStr.indexOf(" user")).toDouble * 1e6
		}).sorted.apply(repeatTimes / 2) // Take the median of 5 runs

		executionTime
	}
}
