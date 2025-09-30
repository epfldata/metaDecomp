package experiments

import java.io.File
import scala.concurrent.duration.Duration
import scala.concurrent.duration.*
import java.nio.file.Files
import java.nio.file.Paths

case object Config {
	private def findProjectRoot(start: File = new File("."): File): Option[File] = {
		if (new File(start, "build.sbt").exists) Some(start.getAbsoluteFile)
		else Option(start.getParentFile).flatMap(findProjectRoot)
	}

	val projectRootPath: String = findProjectRoot().map(_.getPath).get

	val benchmarksPath = s"$projectRootPath/benchmarks"
	val benchmarks: List[String] = List("job-original", "job-large", "dsb", "musicbrainz")

	def dbFilePath(benchmark: String): String = benchmark match {
		case "job-original" | "job-large" => s"$projectRootPath/datasets/imdb/imdb.db"
		case "dsb"          => s"$projectRootPath/datasets/dsb/dsb-10g.db"
		case "musicbrainz"  => s"$projectRootPath/datasets/musicbrainz/musicbrainz.db"
	}
	def dataSource(benchmark: String): String = s"jdbc:duckdb:${dbFilePath(benchmark)}"

	val resultsDir = s"$projectRootPath/experiment-results"

	val repeatTimes = 10
	val timeout: Duration = 300.seconds

	def sqlFilesInBenchmark(benchmark: String): Array[File] =
		new java.io.File(s"$benchmarksPath/$benchmark/queries").listFiles
			.filter(_.getName.endsWith(".sql"))
			.sortBy(_.getName)

	def cardinalityEstimationVariants(benchmark: String): List[String] = benchmark match {
		case "job-original" => List("exact")
		case "job-large"    => List("estimate", "all-0")
		case "dsb"          => List("exact")
		case "musicbrainz"  => List("estimate")
	}
}
