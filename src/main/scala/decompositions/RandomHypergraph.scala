package decompositions

import scala.util.Random
import Hypergraph.{Hyperedge, Vertex}

import scala.collection.mutable

object RandomHypergraph {

  /** Generate a random hypergraph with `m` edges on `n` vertices,
   * each edge containing exactly `c` distinct vertices, then reduce
   * it to its largest connected component.
   *
   * @param n                    number of vertices
   * @param m                    number of edges
   * @param c                    size of each hyperedge (must be <= n)
   * @param allowDuplicateEdges  if false, all edges are distinct vertex-sets
   * @param seed                 optional seed for reproducibility
   */
  def generate(
                n: Int,
                m: Int,
                c: Int,
                allowDuplicateEdges: Boolean = true,
                seed: Option[Long] = None
              ): Hypergraph = {
    require(n > 0, "n must be positive")
    require(c > 0 && c <= n, "c must satisfy 0 < c <= n")
    require(m >= 0, "m must be non-negative")

    val rng = seed.map(new Random(_)).getOrElse(new Random())

    var vertices: Vector[Vertex] = (1 to n).map(i => Vertex(s"v$i")).toVector

    // Partial Fisher-Yates: picks c distinct vertices out of n in O(c) time.
    def randomEdgeNodes(): Set[Vertex] = {
      val arr = vertices.toArray
      var i = 0
      while (i < c) {
        val j = i + rng.nextInt(n - i)
        val tmp = arr(i)
        arr(i) = arr(j)
        arr(j) = tmp
        i += 1
      }
      arr.take(c).toSet
    }

    def combinations(n: Int, k: Int): BigInt = {
      val kk = math.min(k, n - k)
      (0 until kk).foldLeft(BigInt(1)) { case (acc, i) => acc * (n - i) / (i + 1) }
    }

    var edgeNodeSets: Vector[Set[Vertex]] =
      if (allowDuplicateEdges) {
        Vector.fill(m)(randomEdgeNodes())
      } else {
        val maxPossible = combinations(n, c)
        require(
          BigInt(m) <= maxPossible,
          s"Cannot generate $m distinct edges of size $c from $n vertices (max possible = $maxPossible)"
        )
        val acc = scala.collection.mutable.LinkedHashSet.empty[Set[Vertex]]
        while (acc.size < m) {
          acc += randomEdgeNodes()
        }
        acc.toVector
      }

    val hyperedges: Set[Hyperedge] = edgeNodeSets.zipWithIndex.map { case (nodes, idx) =>
      vertices = vertices :+ Vertex(s"e${idx}_unique_attribute")
      Hyperedge(nodes + Vertex(s"e${idx}_unique_attribute"), tableName = s"e$idx", alias = s"e$idx")
    }.toSet

    val fullHypergraph = Hypergraph(vertices.toSet, hyperedges)

    largestConnectedComponent(fullHypergraph)
  }

  /** Restrict a hypergraph to its largest connected component
   * (largest by number of vertices). Isolated/disconnected vertices
   * and edges entirely outside that component are dropped.
   */
  private def largestConnectedComponent(hg: Hypergraph): Hypergraph = {
    if (hg.vertices.isEmpty) return hg

    // componentsInducedBy(Set.empty, hg.vertices) partitions all of
    // hg.vertices into connected components w.r.t. adjVertices.
    val components = hg.componentsInducedBy(Set.empty, hg.vertices)

    val biggest = components.maxBy(_.size)

    val keptEdges = hg.edges.filter(e => e.nodes.subsetOf(biggest))

    Hypergraph(biggest, keptEdges)
  }
}