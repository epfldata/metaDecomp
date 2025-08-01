package experiments

import java.io.File
import scala.concurrent.duration.Duration
import scala.concurrent.duration.*

case object Config {
	private def findProjectRoot(start: File = new File("."): File): Option[File] = {
		if (new File(start, "build.sbt").exists) Some(start.getAbsoluteFile)
		else Option(start.getParentFile).flatMap(findProjectRoot)
	}

	val projectRootPath: String = findProjectRoot().map(_.getPath).get

	val benchmarksPath = s"$projectRootPath/benchmarks"
	val benchmarks: List[String] = List("job-original", "job-large")

	val dbFilePath = s"$projectRootPath/datasets/imdb/imdb.db"
	val dataSource = s"jdbc:duckdb:$dbFilePath"

	val resultsDir = s"$projectRootPath/experiment-results"

	val repeatTimes = 10
	val timeout: Duration = 300.seconds

	def sqlFilesInBenchmark(benchmark: String): Array[File] =
		new java.io.File(s"$benchmarksPath/$benchmark/queries").listFiles
			.filter(_.getName.endsWith(".sql"))
			.sortBy(_.getName)

	def cardinalityEstimationVariants(benchmark: String): List[String] = {
		if benchmark == "job-original" then List("exact") else List("estimate", "all-0")
	}
}
