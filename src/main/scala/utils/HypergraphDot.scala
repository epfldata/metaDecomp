package utils

import decompositions.Hypergraph
import decompositions.Hypergraph.Vertex

import scala.collection.mutable

/** Graphviz rendering with one rounded cluster per hyperedge.
  *
  * A shared vertex is copied into every incident hyperedge cluster. Dashed
  * lines join those copies, making equality/shared-attribute relationships
  * explicit without requiring geometrically overlapping Graphviz clusters.
  */
object HypergraphDot {
  def render(
      hypergraph: Hypergraph,
      vertexDescriptions: Map[String, String] = Map.empty
  ): String = {
    val edges = hypergraph.edges.toVector.sortBy(edge => (edge.alias, edge.tableName))
    val occurrences = mutable.Map.empty[Vertex, mutable.ArrayBuffer[String]]
    val builder = StringBuilder()

    builder ++= "graph hypergraph {\n"
    builder ++= "  graph [rankdir=LR, bgcolor=\"white\", compound=true, " +
      "overlap=false, splines=polyline, nodesep=0.22, ranksep=0.7, pad=0.2];\n"
    builder ++= "  node [shape=circle, fixedsize=true, width=0.34, height=0.34, " +
      "fontname=\"Helvetica\", fontsize=9, style=filled, fillcolor=\"#ffffff\", " +
      "color=\"#3f6f9f\", penwidth=1.2];\n"
    builder ++= "  edge [color=\"#9aa5b1\", penwidth=1.0];\n"

    edges.zipWithIndex.foreach { case (edge, edgeIndex) =>
      builder ++= s"  subgraph cluster_$edgeIndex {\n"
      builder ++= s"    label=\"${escape(edge.alias)}\";\n"
      builder ++= "    labelloc=\"t\"; fontname=\"Helvetica-Bold\"; fontsize=11;\n"
      builder ++= "    style=\"rounded,filled\"; color=\"#7fa6cc\"; " +
        "fillcolor=\"#edf5fc\"; penwidth=1.5; margin=14;\n"

      val vertices = edge.nodes.toVector.sortBy(_.name)
      if (vertices.isEmpty) {
        builder ++= s"    empty_$edgeIndex [shape=plaintext, fixedsize=false, label=\"∅\"];\n"
      } else {
        vertices.zipWithIndex.foreach { case (vertex, vertexIndex) =>
          val nodeId = s"e${edgeIndex}_v$vertexIndex"
          val tooltip = vertexDescriptions.getOrElse(vertex.name, vertex.name)
          builder ++= s"    $nodeId [label=\"${escape(vertex.name)}\", " +
            s"tooltip=\"${escape(tooltip)}\"];\n"
          occurrences.getOrElseUpdate(vertex, mutable.ArrayBuffer.empty) += nodeId
        }
        vertices.indices.sliding(2).foreach { pair =>
          if (pair.size == 2) {
            val left = pair.head
            val right = pair.last
            builder ++= s"    e${edgeIndex}_v$left -- e${edgeIndex}_v$right " +
              "[style=invis, weight=8];\n"
          }
        }
      }
      builder ++= "  }\n"
    }

    occurrences.toVector.sortBy(_._1.name).foreach { case (_, copies) =>
      copies.sliding(2).foreach { pair =>
        if (pair.size == 2) {
          val left = pair.head
          val right = pair.last
          builder ++= s"  $left -- $right [style=dashed, color=\"#65758b\", " +
            "penwidth=1.2, constraint=true];\n"
        }
      }
    }
    builder ++= "}\n"
    builder.result()
  }

  private def escape(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")
}
