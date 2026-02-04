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

object DuckDBRunner extends BaseRunner {
	def main(args: Array[String]): Unit = {
		for (benchmark <- benchmarks) {
			connect(benchmark)

			val resultsPath = Paths.get(resultsDir, s"duckdb-$benchmark-$getTimestamp.csv")

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

				val query = readInOneLine(sqlFile)

				val command = Seq("./duckdb/build/release/duckdb", "-readonly", dbFilePath(benchmark), "-c", "EXPLAIN " + query)

				val optTime = (for (i <- 0 until repeatTimes) yield {
					val output = mutable.ListBuffer[String]()
					val process = Process(command).run(ProcessLogger(fout = output.addOne, ferr = output.addOne))
					val future = Future(process.exitValue())
					val exitCode = Await.result(future, timeout)
					output.find(line => line.contains("Join order optimization time: ")).get.stripPrefix("Join order optimization time: ").stripSuffix(" us").toDouble
				}).median()

				println(s"Optimization time: $optTime us")

				val totalTime = runQuery(query).toDouble
				val execTime = if totalTime < timeout.toMicros - 1e-5 then totalTime - optTime else timeout.toMicros
				println(s"Execution time: $execTime us")
				println(s"Total time: $totalTime us")


				Files.write(resultsPath, s"${sqlFile.getName.stripSuffix(".sql")},$optTime,$execTime,$totalTime\n".getBytes, StandardOpenOption.APPEND)
			)

			conn.close()
		}

	}
}
