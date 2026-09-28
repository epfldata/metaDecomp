package experiments.runner

import decompositions.{Hypergraph, RegularHypertreeDecompositionFinder}
import experiments.Config.{benchmarksPath, resultsDir}
import sql.SQLParser

import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.io.Source

object RegularHypertreeWidthVerificationRunner {
  def main(args: Array[String]): Unit = {
    val fractionalResults = readCsv(
      Paths.get(resultsDir, "fractional-hypertree-width-subgraph-matching.csv").toString
    )
    val targets = fractionalResults.filter { row =>
      val fractional = row("fhw_decimal").toDouble
      val pfnfhw = row("pfnfhw").toInt
      math.abs(fractional - math.rint(fractional)) < 1e-9 && fractional < pfnfhw
    }

    val output = Paths.get(resultsDir, "regular-hypertree-width-verification-subgraph-matching.csv")
    Files.writeString(
      output,
      "query,fhw,hw,pfnfhw,decomposition_valid," +
        "decomposition_projection_free,non_projection_free_nodes\n",
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE,
      StandardOpenOption.TRUNCATE_EXISTING
    )
    val nodeOutput = Paths.get(
      resultsDir,
      "regular-hypertree-decompositions-subgraph-matching.csv"
    )
    Files.writeString(
      nodeOutput,
      "query,node_id,parent_id,chi_attributes,lambda_hyperedges," +
        "lambda_union_attributes,projection_free\n",
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE,
      StandardOpenOption.TRUNCATE_EXISTING
    )

    val finder = RegularHypertreeDecompositionFinder()
    targets.foreach { row =>
      val queryName = row("query")
      val source = Source.fromFile(
        Paths.get(benchmarksPath, "subgraph-matching", "queries", s"$queryName.sql").toFile
      )
      val query = try source.mkString finally source.close()
      val sqlIR = SQLParser.parse(query)
      val parsed = Hypergraph(sqlIR.hyperedges.flatMap(_.nodes), sqlIR.hyperedges)
      val hypergraph = parsed.withoutUniqueAttributes
      val fractionalWidth = row("fhw_decimal").toDouble
      val targetWidth = math.rint(fractionalWidth).toInt
      val decomposition = finder.find(hypergraph, targetWidth).getOrElse(
        throw new IllegalStateException(s"No regular width-$targetWidth decomposition for $queryName")
      )
      val validation = decomposition.validate(hypergraph)
      require(validation.isRight, s"Invalid decomposition for $queryName: $validation")
      require(
        decomposition.width == targetWidth,
        s"Unexpected regular width ${decomposition.width} for $queryName"
      )
      require(!decomposition.isProjectionFree, s"Width-$targetWidth witness for $queryName is projection-free")

      val pfnfhw = row("pfnfhw").toInt
      Files.writeString(
        output,
        s"$queryName,$targetWidth,${decomposition.width},$pfnfhw,true,false," +
          s"${decomposition.nonProjectionFreeNodes.size}\n",
        StandardOpenOption.APPEND
      )
      def writeNodes(
          node: decompositions.RegularHypertreeDecompositionNode,
          parentId: Option[Int]
      ): Unit = {
        val chi = node.chi.toVector.map(_.name).sorted.mkString("|")
        val lambda = node.lambda.toVector.map(_.alias).sorted.mkString("|")
        val lambdaUnion = node.lambda.flatMap(_.nodes).toVector.map(_.name).sorted.mkString("|")
        val projectionFree = node.chi == node.lambda.flatMap(_.nodes)
        Files.writeString(
          nodeOutput,
          s"$queryName,${node.id},${parentId.map(_.toString).getOrElse("")}," +
            s"$chi,$lambda,$lambdaUnion,$projectionFree\n",
          StandardOpenOption.APPEND
        )
        node.children.foreach(child => writeNodes(child, Some(node.id)))
      }
      writeNodes(decomposition.root, None)
      println(
        s"$queryName: regular hw=${decomposition.width}, pfnfhw=$pfnfhw, " +
          s"non-projection-free nodes=${decomposition.nonProjectionFreeNodes.size}"
      )
    }
  }

  private def readCsv(path: String): Vector[Map[String, String]] = {
    val source = Source.fromFile(path)
    try {
      val lines = source.getLines()
      val header = lines.next().split(",", -1).toVector
      lines.filter(_.nonEmpty).map { line =>
        header.zip(line.split(",", -1)).toMap
      }.toVector
    } finally source.close()
  }
}
