package decompositions

import sql.IR
import decompositions.Hypergraph.{Vertex, Hyperedge, HyperedgeSetExtension, Separator, Component}
import decompositions.MetaDecompGraph
import utils._
import scala.collection.mutable
import scala.util.control.Breaks.{break, breakable}
import scala.collection.immutable.HashSet

class MetaDecompGraphConstructInterpolatable {

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
          val edgesInC = H.edgesInComponent(c)
          val requiredNodes = edgesInC.nodes.intersect(separatorNodes)

          val interpolatableCandidates =  {
            val pool = H.edges.filter(_.nodes.subsetOf(separatorNodes ++ c))
            val outside = pool -- separator // candidate edges to add (not already in separator)

            val addOnly: Iterator[Separator] =
              if (separator.size < width) outside.iterator.map(e2 => separator + e2)
              else Iterator.empty

            val replace: Iterator[Separator] =
              for (e1 <- separator.iterator; e2 <- outside.iterator)
                yield (separator - e1) + e2


            (addOnly ++ replace)
              .filter { s =>
                edgesInC.intersect(s).nonEmpty && // S' makes progress in C
                  requiredNodes.subsetOf(s.nodes) // No escape path
              }
              .filter(s => H.isConnected(separator ++ s) && H.isConnected(s ++ edgesInC))

          }

          breakable {
            for (sp <- interpolatableCandidates) {
              rec(c -- sp.nodes, sp, depth + 1) match {
                case true =>
                  successful = true
                  graph.addEdge(separator, sp, c)
                case false =>
                // Continue to try the next S'
              }
            }
          }
          if (!successful) {
            // also need to try other separators
            val candidates =  {
              H.edges.filter(_.nodes.subsetOf(separatorNodes ++ c))
                .subsetsOfSizeAtMost(width)
                .filter(s => !interpolatableCandidates.contains(s)) // don't want to check interpolatable separators twice
                .filter { s =>
                  edgesInC.intersect(s).nonEmpty && // S' makes progress in C
                    requiredNodes.subsetOf(s.nodes) // No escape path
                }
                .filter(s => H.isConnected(separator ++ s) && H.isConnected(s ++ edgesInC))

            }
            breakable {
              for (sp <- interpolatableCandidates) {
                rec(c -- sp.nodes, sp, depth + 1) match {
                  case true =>
                    successful = true
                    graph.addEdge(separator, sp, c)
                    break // stop if at least one working separator found; don't want to add to much non-interp. edges
                  case false =>
                  // Continue to try the next S'
                }
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