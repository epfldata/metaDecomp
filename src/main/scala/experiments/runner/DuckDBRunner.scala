package experiments.runner

import experiments.Config.*
import experiments.readInOneLine

import java.nio.file.{Files, Paths, StandardOpenOption}

object DuckDBRunner extends BaseRunner {
	def main(args: Array[String]): Unit = {
		val benchmark = "job-large"

		val resultsPath = Paths.get(resultsDir, s"duckdb-$benchmark.csv")

		Files.write(
			resultsPath,
			"query,total_time\n".getBytes,
			StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
		)


		sqlFilesInBenchmark(benchmark).foreach(sqlFile =>
			println("\n-----------------------------------")
			println(s"${sqlFile.getName}")

			val query = readInOneLine(sqlFile)

			val time = runQuery(query)

			println(s"Total time: $time us")

			Files.write(resultsPath, s"${sqlFile.getName.stripSuffix(".sql")},$time\n".getBytes, StandardOpenOption.APPEND)
		)

		conn.close()

	}
}
