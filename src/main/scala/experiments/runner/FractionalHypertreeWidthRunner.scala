package experiments.runner

import decompositions.{FractionalHypertreeWidth, Hypergraph, RegularHypertreeDecompositionFinder}
import experiments.Config.{resultsDir, sqlFilesInBenchmark}
import sql.SQLParser

import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.collection.mutable
import scala.io.Source
import scala.sys.process.*

object FractionalHypertreeWidthRunner {
  private val Benchmarks = List(
    "dsb-cyclic",
    "musicbrainz-cyclic",
    "subgraph-matching",
    "custom-ar3-new"
  )

  private val CompleteResultFiles = Map(
    "dsb-cyclic" -> "metadecomp-complete-opt-dsb-cyclic.csv",
    "musicbrainz-cyclic" -> "metadecomp-complete-opt-musicbrainz-cyclic.csv",
    "subgraph-matching" -> "metadecomp-complete-opt-subgraph-matching.csv",
    "custom-ar3-new" -> "metadecomp-complete-opt-custom.csv"
  )

  def main(args: Array[String]): Unit = {
    val selected = if (args.nonEmpty) args.toList else Benchmarks
    selected.foreach { benchmark =>
      require(Benchmarks.contains(benchmark), s"Unknown benchmark: $benchmark")
      runBenchmark(benchmark)
    }
  }

  private def runBenchmark(benchmark: String): Unit = {
    val pfnfhwByQuery = readProjectionFreeNormalFormWidths(
      Paths.get(resultsDir, CompleteResultFiles(benchmark)).toString
    )
    val output = Paths.get(resultsDir, s"fractional-hypertree-width-$benchmark.csv")
    Files.writeString(
      output,
      "query,fhw_decimal,fhw_fraction,pfnfhw,hw\n",
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE,
      StandardOpenOption.TRUNCATE_EXISTING
    )

    val visualizationDir = Paths.get(
      resultsDir,
      "fractional-hypertree-decompositions",
      benchmark
    )
    Files.createDirectories(visualizationDir)
    val calculator = FractionalHypertreeWidth()
    val regularFinder = RegularHypertreeDecompositionFinder()
    val decompositionCache = mutable.Map.empty[
      String,
      (Double, decompositions.FractionalHypertreeDecomposition)
    ]
    val regularWidthCache = mutable.Map.empty[String, Int]
    val sqlFiles = sqlFilesInBenchmark(benchmark)
    sqlFiles.zipWithIndex.foreach { case (sqlFile, index) =>
      val queryName = sqlFile.getName.stripSuffix(".sql")
      val source = Source.fromFile(sqlFile)
      val query = try source.mkString finally source.close()
      val sqlIR = SQLParser.parse(query)
      val parsed = Hypergraph(sqlIR.hyperedges.flatMap(_.nodes), sqlIR.hyperedges)
      val stripped = parsed.withoutUniqueAttributes
      val signature = hypergraphSignature(stripped)
      val (fractionalWidth, decomposition) = decompositionCache.getOrElseUpdate(
        signature,
        calculator.runWithDecomposition(stripped)
      )
      val pfnfhw = pfnfhwByQuery.getOrElse(
        queryName,
        throw new IllegalArgumentException(
          s"No complete projection-free normal-form width for $benchmark/$queryName"
        )
      )
      require(
        fractionalWidth <= pfnfhw + 1e-8,
        s"FHW $fractionalWidth exceeds PFNFHW $pfnfhw " +
          s"for $benchmark/$queryName"
      )
      val regularWidth = regularWidthCache.getOrElseUpdate(
        signature, {
          val lowerBound = math.ceil(fractionalWidth - 1e-8).toInt
          (lowerBound to pfnfhw).iterator
            .flatMap(width => regularFinder.find(stripped, width))
            .nextOption()
            .map(_.width)
            .getOrElse(
              throw new IllegalStateException(
                s"No regular hypertree decomposition of width at most " +
                  s"$pfnfhw for $benchmark/$queryName"
              )
            )
        }
      )
      require(
        fractionalWidth <= regularWidth + 1e-8 && regularWidth <= pfnfhw,
        s"Expected fhw <= hw <= pfnfhw, got $fractionalWidth <= " +
          s"$regularWidth <= $pfnfhw for $benchmark/$queryName"
      )

      val fraction = approximateFraction(fractionalWidth)
      val decimal = if (fraction._2 == 1) fraction._1.toString else fractionalWidth.toString
      val fractionString =
        if (fraction._2 == 1) fraction._1.toString else s"${fraction._1}/${fraction._2}"
      Files.writeString(
        output,
        s"$queryName,$decimal,$fractionString,$pfnfhw,$regularWidth\n",
        StandardOpenOption.APPEND
      )
      val dotPath = visualizationDir.resolve(s"$queryName.dot")
      val svgPath = visualizationDir.resolve(s"$queryName.svg")
      Files.writeString(
        dotPath,
        decomposition.toDot,
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING
      )
      val dotExitCode = Seq("dot", "-Tsvg", dotPath.toString, "-o", svgPath.toString).!
      require(dotExitCode == 0, s"Graphviz failed for $benchmark/$queryName")
      println(
        s"[$benchmark] ${index + 1}/${sqlFiles.length} " +
          s"$queryName: fhw=$fractionString, pfnfhw=$pfnfhw, " +
          s"hw=$regularWidth"
      )
    }
  }

  private def readProjectionFreeNormalFormWidths(path: String): Map[String, Int] = {
    val source = Source.fromFile(path)
    try {
      val lines = source.getLines()
      val header = lines.next().split(",", -1)
      val queryIndex = header.indexOf("query")
      val widthIndex = header.indexOf("width")
      require(queryIndex >= 0 && widthIndex >= 0, s"Missing query/width columns in $path")
      lines.filter(_.nonEmpty).map { line =>
        val fields = line.split(",", -1)
        fields(queryIndex) -> fields(widthIndex).toInt
      }.toMap
    } finally source.close()
  }

  private def hypergraphSignature(hypergraph: Hypergraph): String =
    hypergraph.edges.toVector
      .map(edge => edge.nodes.toVector.map(_.name).sorted.mkString("(", ",", ")"))
      .sorted
      .mkString

  private def approximateFraction(value: Double, maxDenominator: Int = 1000000): (Long, Long) = {
    var bestNumerator = math.round(value)
    var bestDenominator = 1L
    var leftNumerator = 0L
    var leftDenominator = 1L
    var rightNumerator = 1L
    var rightDenominator = 0L
    var done = false
    while (!done) {
      val middleNumerator = leftNumerator + rightNumerator
      val middleDenominator = leftDenominator + rightDenominator
      if (middleDenominator > maxDenominator) {
        done = true
      } else {
        val middle = middleNumerator.toDouble / middleDenominator
        if (math.abs(middle - value) < math.abs(bestNumerator.toDouble / bestDenominator - value)) {
          bestNumerator = middleNumerator
          bestDenominator = middleDenominator
        }
        if (math.abs(middle - value) < 1e-10) done = true
        else if (middle < value) {
          leftNumerator = middleNumerator
          leftDenominator = middleDenominator
        } else {
          rightNumerator = middleNumerator
          rightDenominator = middleDenominator
        }
      }
    }
    (bestNumerator, bestDenominator)
  }
}
