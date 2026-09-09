package decompositions

import sql.IR
import decompositions.Hypergraph.{Vertex, Hyperedge, HyperedgeSetExtension, Separator, Component}
import decompositions.MetaDecompGraph
import utils._
import scala.collection.mutable
import scala.util.control.Breaks.{break, breakable}
import scala.collection.immutable.HashSet
import scala.util.Random
import dotty.tools.dotc.util.SimpleIdentitySet.empty

class MetaDecompGraphRandomizedConstructor {

  class Hypertree(val root: Separator) {
    var children = mutable.Map.empty[Component, Hypertree]
  }

  def run(hypergraph: Hypergraph, width: Int): MetaDecompGraph = {
    val startTime = System.nanoTime()
    def timedOut = (System.nanoTime() - startTime) / 1000 > 50 * math.pow(hypergraph.edges.size, 2) * math.pow(5, width - 2)
    // 500 * hypergraph.edges.size * math.pow(hypergraph.edges.size / 4, width - 2)

    val unexhaustedCandidates = mutable.Map.empty[(Separator, Separator, Component), mutable.ListBuffer[Separator]]
    val hasSolution = mutable.Map.empty[(Separator, Separator), Boolean]
    val unaddedSolution = mutable.Map.empty[(Separator, Separator), Hypertree]
    // To store subtrees that are part of a failed run, i.e., not yet added to the meta-decomposition graph, to be used if the same subproblem is encountered again.
    val exhausted = mutable.Set.empty[(Separator, Separator)] // Subproblems of which all possible descendents have been explored.

    val graph = new MetaDecompGraph()
    val H = hypergraph

    def rec(prevSep: Separator, currComp: Set[Vertex], currSep: Separator, depth: Int)(implicit width: Int): Option[Hypertree] = {
      if (unaddedSolution.contains((prevSep, currSep))) {
        return Some(unaddedSolution((prevSep, currSep)))
      } else if (exhausted.contains((prevSep, currSep)) && hasSolution((prevSep, currSep))) {
        return Some(Hypertree(currSep)) // All possible descendent edges have already been added. No need to add them again.
      }
      if (!hasSolution.getOrElse((prevSep, currSep), true)) {
        return None
      }

      if (timedOut) return None

      val separatorNodes = currSep.nodes // hoisted: avoid recomputing per component / per candidate

      val components = H.componentsInducedBy(separatorNodes).filter(_.subsetOf(currComp))
      var allExhausted = true
      val tree = Hypertree(currSep)
      components.foreach { nextComp =>
        if (timedOut) return None
        var successful = false
        val candidates = unexhaustedCandidates.getOrElseUpdate((prevSep, currSep, nextComp), H.enumNextSeparatorCandidates(currSep, nextComp, width))

        breakable { while (candidates.nonEmpty) {
          if (timedOut) return None
          val i = Random.nextInt(candidates.size)
          val nextSep = candidates(i)
          rec(currSep, nextComp, nextSep, depth + 1) match {
            case Some(subtree) =>
              successful = true
              tree.children += (nextComp, subtree)
              if (exhausted.contains((currSep, nextSep))) {
                candidates.remove(i)
              }
              break()
            case None =>
              candidates.remove(i)
            // Continue to try the next S'
          }
        } }
        if (candidates.nonEmpty) {
          allExhausted = false
        }

        if (!successful) { // Some component is not successful
          hasSolution((prevSep, currSep)) = false
          return None
        }
      }
      hasSolution((prevSep, currSep)) = true
      unaddedSolution((prevSep, currSep)) = tree
      if (allExhausted) {
        exhausted += ((prevSep, currSep))
      }
      Some(tree)
    }

    def addGraphEdges(prevSep: Separator, component: Component, subtree: Hypertree): Unit = {
      graph.addEdge(prevSep, subtree.root, component)
      unaddedSolution.remove((prevSep, subtree.root))
      subtree.children.foreach { case (c, s) => addGraphEdges(subtree.root, c, s) }
    }

    // iterate over all possible roots
    val possibleRoots = H.edges.subsetsOfSizeAtMost(width).filter(_.nonEmpty).toIndexedSeq
    val emptySeparator = Set.empty[Hyperedge]

    breakable { for (root <- possibleRoots) {
      if (timedOut) break
      val tree = new Hypertree(root)
      rec(emptySeparator, H.vertices, root, 1)(width) match {
        case Some(tree) => addGraphEdges(emptySeparator, H.vertices, tree)
        case None => // continue
      }
    } }

    val incompleteRoots = mutable.ListBuffer.from(possibleRoots.filterNot(s => exhausted.contains((emptySeparator, s))))

    while (!timedOut && incompleteRoots.nonEmpty) {
      val i = Random.nextInt(incompleteRoots.size)
      val root = incompleteRoots(i)
      rec(emptySeparator, H.vertices, root, 1)(width) match {
        case Some(tree) =>
          addGraphEdges(emptySeparator, H.vertices, tree)
          if (exhausted.contains((emptySeparator, root))) {
            incompleteRoots.remove(i)
          }
        case None => incompleteRoots.remove(i)
      }
    }

    return graph
  }
}
