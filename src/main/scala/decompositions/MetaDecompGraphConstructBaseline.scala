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
          val candidates =  {
            val edgesInC = H.edgesInComponent(c)
            val requiredNodes = edgesInC.nodes.intersect(separatorNodes)
            H.edges.filter(_.nodes.subsetOf(separatorNodes ++ c))
              .subsetsOfSizeAtMost(width)
              .filter { s =>
                edgesInC.intersect(s).nonEmpty && // S' makes progress in C
                  requiredNodes.subsetOf(s.nodes) // No escape path
              }
              .filter(s => H.isConnected(separator ++ s) && H.isConnected(s ++ edgesInC))

          }

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
    val reachableSeparators = mutable.Set.empty[Separator]
    val visitedStates =
      mutable.Set.empty[(Separator, Component)]

    val reachabilityQueue =
      mutable.Queue.empty[(Separator, Component)]

    reachabilityQueue.enqueue((root, hypergraph.vertices))

    while (reachabilityQueue.nonEmpty) {
      val state@(r, cr) = reachabilityQueue.dequeue()

      if (visitedStates.add(state)) {
        reachableSeparators += r

        for {
          (cs, successors) <- graph.adjList.getOrElse(
            r,
            mutable.Map.empty[Component, mutable.Set[Separator]]
          )
          if cs.subsetOf(cr)
          s <- successors
        } {
          reachabilityQueue.enqueue((s, cs))
        }
      }
    }

    graph.vertices
      .filterNot(reachableSeparators.contains)
      .foreach(graph.removeVertex)

    return graph
  }
}