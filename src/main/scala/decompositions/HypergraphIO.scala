package decompositions

import java.io.{PrintWriter, File}
import scala.io.Source
import Hypergraph.{Vertex, Hyperedge}

object HypergraphIO {

  /** Write a hypergraph to a file, one edge per line:
   *   edge_name,v1_name,v2_name,...,vk_name
   */
  def writeToFile(hypergraph: Hypergraph, filePath: String): Unit = {
    val writer = new PrintWriter(new File(filePath))
    try {
      hypergraph.edges.foreach { edge =>
        val edgeName = if (edge.alias.nonEmpty) edge.alias else edge.tableName
        val vertexNames = edge.nodes.map(_.name).toList.sorted
        val line = (edgeName +: vertexNames).mkString(",")
        writer.println(line)
      }
    } finally {
      writer.close()
    }
  }

  /** Construct a hypergraph from a file in the format:
   *   edge_name,v1_name,v2_name,...,vk_name
   * (one edge per line)
   */
  def readFromFile(filePath: String): Hypergraph = {
    val source = Source.fromFile(filePath)
    try {
      val lines = source.getLines().map(_.trim).filter(_.nonEmpty).toList

      val edges: Set[Hyperedge] = lines.map { line =>
        val parts = line.split(",").map(_.trim)
        require(parts.length >= 2, s"Malformed line (need edge name + at least one vertex): $line")

        val edgeName = parts(0)
        val vertexNames = parts.drop(1)
        val nodes = vertexNames.map(Vertex(_)).toSet

        Hyperedge(nodes, tableName = edgeName, alias = edgeName)
      }.toSet

      val vertices: Set[Vertex] = edges.flatMap(_.nodes)

      Hypergraph(vertices, edges)
    } finally {
      source.close()
    }
  }
}