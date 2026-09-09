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
	val benchmarks: List[String] = List("dsb", "job-original", "job-large", "musicbrainz")

	def dbFilePath(benchmark: String): String = benchmark match {
		case "job-original" | "job-large" => s"$projectRootPath/datasets/imdb/imdb.db"
		case "dsb" | "dsb-cyclic"          => s"$projectRootPath/datasets/dsb/dsb-10g.db"
		case "musicbrainz" | "musicbrainz-cyclic"  => s"$projectRootPath/datasets/musicbrainz/musicbrainz.db"
		case "custom-ar3-new" | "custom-ar3-extra" => s"$projectRootPath/datasets/custom-ar3-new-range1000-rowcnt10000/custom-ar3-new-range1000-rowcnt10000.db"
		case "subgraph-matching" => s"$projectRootPath/datasets/subgraph-matching/subgraph-matching-downsampled.db"
	}
	def dataSource(benchmark: String): String = s"jdbc:duckdb:${dbFilePath(benchmark)}"

	val resultsDir = s"$projectRootPath/experiment-results"

	val repeatTimes = 10
	val timeout: Duration = 300.seconds

	def sqlFilesInBenchmark(benchmark: String): Array[File] = {
		new java.io.File(s"$benchmarksPath/$benchmark/queries").listFiles
			.filter(_.getName.endsWith(".sql"))
			.sortBy(_.getName)
	}
}
