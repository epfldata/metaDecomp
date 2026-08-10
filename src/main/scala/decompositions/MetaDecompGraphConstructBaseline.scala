package decompositions

import sql.IR
import decompositions.Hypergraph.{Vertex, Hyperedge, HyperedgeSetExtension, Separator, Component}
import decompositions.MetaDecompGraph
import utils._
import scala.collection.mutable
import scala.util.control.Breaks.{break, breakable}
import scala.collection.immutable.HashSet

class MetaDecompGraphConstructBaseline {

  def run(hypergraph: Hypergraph, width: Int): MetaDecompGraph = {
    val graph = new MetaDecompGraph()
    val H = hypergraph

    val nextSeparatorCandidates = mutable.Map[(Separator, Component), Array[Separator]]()
    val memo = mutable.Map.empty[(Set[Vertex], Separator), Boolean]

    def rec(remainingVertices: Set[Vertex], separator: Separator, depth: Int)(implicit width: Int): Boolean = {
      if (memo.contains((remainingVertices, separator))) {
        return memo((remainingVertices, separator))
      }

      val separatorNodes = separator.nodes // hoisted: avoid recomputing per component / per candidate

      val components = H.componentsInducedBy(separatorNodes).filter(_.subsetOf(remainingVertices))
      var allSuccessful = true
      components.foreach { c =>
        var successful = false

        val candidates = nextSeparatorCandidates.getOrElseUpdate((separator, c), {
          val edgesInC = H.edgesInComponent(c)
          val requiredNodes = edgesInC.nodes.intersect(separatorNodes)
          H.edges.filter(_.nodes.subsetOf(separatorNodes ++ c))
            .subsetsOfSizeAtMost(width)
            .filter { s =>
              edgesInC.intersect(s).nonEmpty &&        // S' makes progress in C
                requiredNodes.subsetOf(s.nodes)           // No escape path
            }.filter(s => H.isConnected(separator ++ s) && H.isConnected(s ++ edgesInC))
            .toArray
        })

        breakable {
          for (sp <- candidates) {
            rec(c -- sp.nodes, sp, depth + 1) match {
              case true =>
                successful = true
                graph.addEdge(separator, sp, c)
              case false =>
              // Continue to try the next S'
            }
          }
        }
        if (!successful) { // Some component is not successful
          allSuccessful = false
          memo((remainingVertices, separator)) = false
        }
      }
      if (allSuccessful) {
        memo((remainingVertices, separator)) = true
      }
      allSuccessful
    }

    // iterate over all possible roots
    for (root <- H.edges.subsetsOfSizeAtMost(width)) {
      if (root.nonEmpty && H.isConnected(root) && rec(H.vertices -- root.nodes, root, 1)(width)) {
        val emptySeparator: Separator = HashSet.empty[Hyperedge]
        graph.addEdge(emptySeparator, root, H.vertices)
      }
    }

    val root = graph.vertices.find(_.isEmpty).get
    val reachableVertices = mutable.Set.empty[Separator]
    val reachabilityQueue = mutable.Queue.empty[(Separator, Component)]
    reachabilityQueue.enqueue((root, hypergraph.vertices))
    while (reachabilityQueue.nonEmpty) {
      val (r, cr) = reachabilityQueue.dequeue()
      reachableVertices += r
      if (graph.adjList.contains(r)) {
        for ((cs, ss) <- graph.adjList(r) if (cs.subsetOf(cr)); s <- ss if (!reachableVertices.contains(s))) {
          reachabilityQueue.enqueue((s, cs))
        }
      }
    }
    graph.vertices.filterNot(reachableVertices.contains).foreach(graph.removeVertex)

    // Debug-only sanity check block removed (optimization #5): it was purely
    // recomputing componentsInducedBy for every edge with all println calls
    // commented out, i.e. 100% wasted work with no observable effect.

    return graph
  }
}