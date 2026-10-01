package decompositions

import decompositions.Hypergraph.{Component, Hyperedge, Separator, Vertex}
import utils.*

import scala.collection.immutable.HashSet
import scala.collection.mutable

/**
 * Output-equivalent implementation of [[MetaDecompGraphConstructBaseline]].
 *
 * The original implementation is intentionally left unchanged so the two
 * constructors can be compared directly.
 */
class MetaDecompGraphConstructBaselineOptimized {

  def run(hypergraph: Hypergraph, width: Int): MetaDecompGraph = {
    val graph = new MetaDecompGraph()
    val H = hypergraph

    val memo = mutable.HashMap.empty[(Set[Vertex], Separator), Boolean]
    val memoComp = mutable.HashMap.empty[(Component, Separator), Boolean]

    // HyperedgeSetExtension's cache belongs to a short-lived implicit wrapper,
    // so retain the union of each immutable separator here for the whole run.
    val nodesMemo = mutable.HashMap.empty[Separator, Set[Vertex]]
    def nodesOf(separator: Separator): Set[Vertex] =
      nodesMemo.getOrElseUpdate(separator, separator.flatMap(_.nodes))

    /** Equivalent to Hypergraph.enumNextSeparatorCandidates, sharing nodesMemo. */
    def enumNextSeparatorCandidates(
        prevSep: Separator,
        component: Component
    ): mutable.ListBuffer[Separator] = {
      val candidatesAcc = mutable.ListBuffer.empty[Separator]
      val edgesOfDist = mutable.IndexedBuffer.empty[Set[Hyperedge]]
      val edgesWithinDist = mutable.IndexedBuffer.empty[Set[Hyperedge]]
      val edgesInC = H.edgesInComponent(component)
      val prevSepNodes = nodesOf(prevSep)
      val interface = nodesOf(edgesInC).intersect(prevSepNodes)

      def conditions(candSep: Separator): Boolean = {
        val candSepNodes = nodesOf(candSep)
        candSep.intersect(edgesInC).nonEmpty &&
        interface.subsetOf(candSepNodes) &&
        (!component.subsetOf(candSepNodes) || H.isConnected(candSep)) &&
        H.isConnected(prevSep ++ candSep)
      }

      def checkAndAddSeparator(candSep: Separator): Boolean = {
        if (conditions(candSep)) {
          candidatesAcc += candSep
          if (component.subsetOf(nodesOf(candSep))) return true
        }
        false
      }

      def enumerateAtDistance(currentSet: Set[Hyperedge], dist: Int): Option[Separator] = {
        if (checkAndAddSeparator(currentSet)) return Some(currentSet)

        if (currentSet.size < width) {
          if (edgesOfDist.size <= dist + 1) {
            edgesOfDist.insert(
              dist + 1,
              edgesOfDist(dist).flatMap { edge =>
                (H.adjEdges(edge) -- edgesWithinDist(dist))
                  .filter(_.nodes.subsetOf(prevSepNodes ++ component))
              }
            )
            edgesWithinDist.insert(dist + 1, edgesWithinDist(dist) ++ edgesOfDist(dist + 1))
          }

          val additionsIterator = edgesOfDist(dist + 1)
            .nonEmptySubsetsOfSizeAtMost(width - currentSet.size)
            .iterator
          while (additionsIterator.hasNext) {
            enumerateAtDistance(currentSet ++ additionsIterator.next(), dist + 1) match {
              case Some(separator) => return Some(separator)
              case None            =>
            }
          }
        }
        None
      }

      if (prevSep.isEmpty || width <= 2) {
        val fullList = mutable.ListBuffer.from(
          H.edges
            .filter(_.nodes.subsetOf(prevSepNodes ++ component))
            .subsetsOfSizeAtMost(width)
            .filter(conditions)
        )
        fullList.find(separator => component.subsetOf(nodesOf(separator))) match {
          case Some(separator) => mutable.ListBuffer(separator)
          case None            => fullList
        }
      } else {
        edgesOfDist.insert(0, prevSep)
        edgesWithinDist.insert(0, edgesOfDist(0))
        val subsetIterator = prevSep.subsetsOfSizeAtMost(width - 1).iterator
        while (subsetIterator.hasNext) {
          enumerateAtDistance(subsetIterator.next(), 0) match {
            case Some(separator) => return mutable.ListBuffer(separator)
            case None            =>
          }
        }
        candidatesAcc
      }
    }

    def rec(remainingVertices: Set[Vertex], separator: Separator): Boolean = {
      val state = (remainingVertices, separator)
      memo.get(state) match {
        case Some(result) => return result
        case None         =>
      }

      val components = H.componentsInducedBy(nodesOf(separator)).filter(_.subsetOf(remainingVertices))
      var allSuccessful = true
      val componentIterator = components.iterator

      while (componentIterator.hasNext) {
        val component = componentIterator.next()
        val componentState = (component, separator)

        val successful = memoComp.get(componentState) match {
          case Some(result) => result
          case None =>
            var foundSuccessful = false
            val successfulPruningFrontier = mutable.ArrayBuffer.empty[Separator]
            val candidates = enumNextSeparatorCandidates(separator, component)
            val candidateIterator = candidates.iterator

            // A successful separator removes itself and all its supersets from
            // the baseline's candidate buffer. Checking the same condition as
            // candidates stream past avoids repeatedly filtering that buffer.
            while (candidateIterator.hasNext) {
              val nextSeparator = candidateIterator.next()
              if (!successfulPruningFrontier.exists(_.subsetOf(nextSeparator))) {
                if (rec(component -- nodesOf(nextSeparator), nextSeparator)) {
                  foundSuccessful = true
                  graph.addEdge(separator, nextSeparator, component)
                  successfulPruningFrontier.filterInPlace(
                    existing => !nextSeparator.subsetOf(existing)
                  )
                  successfulPruningFrontier += nextSeparator
                }
              }
            }

            memoComp(componentState) = foundSuccessful
            foundSuccessful
        }

        if (!successful) allSuccessful = false
      }

      memo(state) = allSuccessful
      allSuccessful
    }

    val emptySeparator: Separator = HashSet.empty[Hyperedge]
    H.edges.subsetsOfSizeAtMost(width).foreach { root =>
      if (root.nonEmpty && rec(H.vertices -- nodesOf(root), root)) {
        graph.addEdge(emptySeparator, root, H.vertices)
      }
    }

    graph.vertices.find(_.isEmpty) match {
      case Some(root) =>
        val reachableVertices = mutable.Set.empty[Separator]
        val reachabilityQueue = mutable.Queue.empty[(Separator, Component)]
        reachabilityQueue.enqueue((root, H.vertices))

        while (reachabilityQueue.nonEmpty) {
          val (separator, containingComponent) = reachabilityQueue.dequeue()
          reachableVertices += separator
          graph.adjList.get(separator).foreach { outgoing =>
            for {
              (component, nextSeparators) <- outgoing
              if component.subsetOf(containingComponent)
              nextSeparator <- nextSeparators
              if !reachableVertices.contains(nextSeparator)
            } reachabilityQueue.enqueue((nextSeparator, component))
          }
        }

        // Preserve the baseline's iteration/mutation behavior exactly. A bulk
        // cleanup is faster, but can remove vertices that the baseline leaves
        // behind because removeVertex mutates graph.vertices during iteration.
        graph.vertices.filterNot(reachableVertices.contains).foreach(graph.removeVertex)

      case None =>
        graph.vertices.foreach(graph.removeVertex)
    }

    graph
  }
}
