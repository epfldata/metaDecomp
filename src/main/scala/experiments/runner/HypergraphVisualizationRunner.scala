package experiments.runner

import decompositions.Hypergraph
import experiments.Config.{resultsDir, sqlFilesInBenchmark}
import sql.SQLParser
import utils.{HypergraphDot, HypergraphSvg}

import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.io.Source
import scala.sys.process.*

object HypergraphVisualizationRunner {
  private val Benchmarks = List(
    "dsb-cyclic",
    "musicbrainz-cyclic",
    "subgraph-matching",
    "custom-ar3-new"
  )

  def main(args: Array[String]): Unit = {
    val selected = if (args.nonEmpty) args.toList else Benchmarks
    selected.foreach { benchmark =>
      require(Benchmarks.contains(benchmark), s"Unknown benchmark: $benchmark")
      val outputDir = Paths.get(resultsDir, "hypergraphs", benchmark)
      Files.createDirectories(outputDir)
      val sqlFiles = sqlFilesInBenchmark(benchmark)

      sqlFiles.zipWithIndex.foreach { case (sqlFile, index) =>
        val queryName = sqlFile.getName.stripSuffix(".sql")
        val source = Source.fromFile(sqlFile)
        val query = try source.mkString finally source.close()
        val sqlIR = SQLParser.parse(query)
        val parsed = Hypergraph(sqlIR.hyperedges.flatMap(_.nodes), sqlIR.hyperedges)
        val stripped = parsed.withoutUniqueAttributes
        val descriptions = sqlIR.vertexIdToColumnName.map { case (vertex, columns) =>
          vertex -> columns.toVector
            .map { case (edge, column) => s"${edge.alias}.$column" }
            .sorted
            .mkString(" = ")
        }

        val dotPath = outputDir.resolve(s"$queryName.dot")
        val svgPath = outputDir.resolve(s"$queryName.svg")
        val pngPath = outputDir.resolve(s"$queryName.png")
        val jpegPath = outputDir.resolve(s"$queryName.jpg")
        Files.writeString(
          dotPath,
          HypergraphDot.render(stripped, descriptions),
          StandardOpenOption.CREATE,
          StandardOpenOption.WRITE,
          StandardOpenOption.TRUNCATE_EXISTING
        )
        Files.writeString(
          svgPath,
          HypergraphSvg.render(stripped, descriptions),
          StandardOpenOption.CREATE,
          StandardOpenOption.WRITE,
          StandardOpenOption.TRUNCATE_EXISTING
        )
        val pngExitCode = Seq(
          "rsvg-convert",
          svgPath.toString,
          "-o",
          pngPath.toString
        ).!
        require(pngExitCode == 0, s"PNG export failed for $benchmark/$queryName")
        val jpegExitCode = Seq(
          "magick",
          pngPath.toString,
          "-quality",
          "90",
          jpegPath.toString
        ).!
        require(jpegExitCode == 0, s"JPEG export failed for $benchmark/$queryName")
        println(s"[$benchmark] ${index + 1}/${sqlFiles.length} $queryName")
      }
    }
  }
}
