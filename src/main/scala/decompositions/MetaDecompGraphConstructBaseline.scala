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
    
    val memo = mutable.Map.empty[(Set[Vertex], Separator), Boolean]
    val memoComp = mutable.Map.empty[(Component, Separator), Boolean] // to memorize if this component can be covered

    def rec(remainingVertices: Set[Vertex], separator: Separator, depth: Int)(implicit width: Int): Boolean = {
      if (memo.contains((remainingVertices, separator))) {
        return memo((remainingVertices, separator))
      }

      val separatorNodes = separator.nodes // hoisted: avoid recomputing per component / per candidate

      val components = H.componentsInducedBy(separatorNodes).filter(_.subsetOf(remainingVertices))
      var allSuccessful = true
      components.foreach { c =>
        var successful = false
        if (memoComp.contains((c, separator))) {
            successful = memoComp((c, separator))
        } else {
          val candidates = H.enumNextSeparatorCandidates(prevSep = separator, component = c, width = width)

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
          memoComp((c, separator)) = successful
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
      if (root.nonEmpty  && rec(H.vertices -- root.nodes, root, 1)(width)
      && H.isConnected(root)
      ) {
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

    return graph
  }
}