package experiments.runner

import decompositions.Hypergraph
import experiments.Config.{benchmarks, benchmarksPath, resultsDir, sqlFilesInBenchmark}
import experiments.getTimestamp
import sql.SQLParser

import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.io.Source

/**
 * Runs the randomized meta-decomposition experiment with interleaved timing
 * and an independent remeasurement of the three fastest inserted candidates.
 */
object MetaDecompRandomizedConstructorFairRunner {
  def main(args: Array[String]): Unit = {
    for (benchmark <- if args.nonEmpty then List(args(0)) else benchmarks) {
      MetaDecompRandomizedConstructorRunner.connect(benchmark)

      val benchmarkPath = s"$benchmarksPath/$benchmark"
      val resultsPath = Paths.get(resultsDir, s"metadecomp-randomized-progress-fair-$benchmark-$getTimestamp.csv")
      Files.write(
        resultsPath,
        "".getBytes,
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING
      )

      sqlFilesInBenchmark(benchmark)
        .filter(file => if args.size >= 2 then file.getName.matches(args(1)) else true)
        .foreach { sqlFile =>
          val queryName = sqlFile.getName.stripSuffix(".sql")
          val source = Source.fromFile(sqlFile)
          val query = try source.getLines().mkString(" ") finally source.close()

          implicit val sqlIR: sql.IR = SQLParser.parse(query)
          val hypergraph = Hypergraph(sqlIR.hyperedges.flatMap(_.nodes).toSet, sqlIR.hyperedges)
          val width = getWidth(hypergraph)

          val extractedSeq = MetaDecompRandomizedConstructorRunner.run(
            hypergraph,
            width,
            benchmarkPath,
            queryName,
            sqlFile,
            query,
            fairMeasurement = true
          )

          Files.write(
            resultsPath,
            (queryName + extractedSeq + "\n").getBytes,
            StandardOpenOption.APPEND
          )
        }

      MetaDecompRandomizedConstructorRunner.conn.close()
    }
  }
}
