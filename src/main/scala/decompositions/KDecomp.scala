package decompositions

import decompositions.Hypergraph.{
  Vertex,
  Hyperedge,
  HyperedgeSetExtension,
  Separator,
  Component
}
import utils._

import scala.collection.mutable

class KDecomp {

  /**
   * Returns true iff the hypergraph has a hypertree decomposition
   * of width <= width satisfying the additional connectedness
   * restrictions used by the MD construction.
   */
  def run(hypergraph: Hypergraph, width: Int): Boolean = {
    val H = hypergraph

    /*
     * solve(C, R):
     *   Can component C, exposed by parent separator R,
     *   be decomposed with width <= k?
     */
    val memo =
      mutable.Map.empty[(Component, Separator), Boolean]

    def solve(
               c: Component,
               separator: Separator
             ): Boolean = {

      memo.get((c, separator)) match {
        case Some(result) =>
          result

        case None =>
          val separatorNodes = separator.nodes

          val edgesInC = H.edgesInComponent(c)

          // Interface between C and its parent separator.
          val requiredNodes =
            edgesInC.nodes.intersect(separatorNodes)

          val candidates =
            H.edges
              .filter(_.nodes.subsetOf(separatorNodes ++ c)) // for projection-free
              .subsetsOfSizeAtMost(width)
              .filter { sp =>
                //  progress condition
                edgesInC.intersect(sp).nonEmpty &&
                  // interface condition
                  requiredNodes.subsetOf(sp.nodes)
              }
              .filter { sp =>
                // our additional connectedness condition
                H.isConnected(separator ++ sp) &&
                  H.isConnected(sp ++ edgesInC)
              }

          val result = candidates.exists { sp =>

            val childComponents =
              H.componentsInducedBy(sp.nodes)
                .filter(_.subsetOf(c))

            childComponents.forall { child =>
              solve(child, sp)
            }
          }

          memo((c, separator)) = result
          result
      }
    }

     //iterate over possible root bags.

    H.edges
      .subsetsOfSizeAtMost(width)
      .filter(_.nonEmpty)
      .filter(H.isConnected)
      .exists { root =>

        val rootComponents =
          H.componentsInducedBy(root.nodes)

        rootComponents.forall { c =>
          solve(c, root)
        }
      }
  }
}
