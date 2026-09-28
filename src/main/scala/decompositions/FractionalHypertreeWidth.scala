package decompositions

import decompositions.Hypergraph.{Hyperedge, Vertex}

import scala.collection.mutable

case class FractionalHypertreeDecompositionNode(
    id: Int,
    chi: Set[Vertex],
    lambda: Map[Hyperedge, Double],
    children: Vector[FractionalHypertreeDecompositionNode]
) {
  def fractionalCoverNumber: Double = lambda.values.sum
}

case class FractionalHypertreeDecomposition(root: FractionalHypertreeDecompositionNode) {
  lazy val nodes: Vector[FractionalHypertreeDecompositionNode] = {
    def collect(node: FractionalHypertreeDecompositionNode): Vector[FractionalHypertreeDecompositionNode] =
      node +: node.children.flatMap(collect)
    collect(root)
  }

  def width: Double = nodes.map(_.fractionalCoverNumber).max

  def validate(hypergraph: Hypergraph, epsilon: Double = 1e-8): Either[String, Unit] = {
    val uncoveredEdges = hypergraph.edges.filterNot(edge => nodes.exists(node => edge.nodes.subsetOf(node.chi)))
    if (uncoveredEdges.nonEmpty)
      return Left(s"query hyperedges are not covered: ${uncoveredEdges.map(_.alias).mkString(",")}")

    val adjacency = mutable.Map.empty[Int, mutable.Set[Int]]
    nodes.foreach(node => adjacency.getOrElseUpdate(node.id, mutable.Set.empty))
    nodes.foreach { node =>
      node.children.foreach { child =>
        adjacency(node.id) += child.id
        adjacency(child.id) += node.id
      }
    }
    val disconnected = hypergraph.vertices.find { vertex =>
      val containing = nodes.filter(_.chi.contains(vertex)).map(_.id).toSet
      if (containing.isEmpty) true
      else {
        val visited = mutable.Set(containing.head)
        val queue = mutable.Queue(containing.head)
        while (queue.nonEmpty) {
          val current = queue.dequeue()
          adjacency(current).filter(containing.contains).filterNot(visited.contains).foreach { next =>
            visited += next
            queue.enqueue(next)
          }
        }
        visited.toSet != containing
      }
    }
    if (disconnected.nonEmpty)
      return Left(s"running-intersection condition fails for vertex ${disconnected.get.name}")

    val failedCover = nodes.iterator.flatMap { node =>
      val undercovered = node.chi.find { vertex =>
        node.lambda.iterator.collect { case (edge, weight) if edge.nodes.contains(vertex) => weight }.sum < 1.0 - epsilon
      }
      undercovered.map(vertex => node.id -> vertex)
    }.nextOption()
    if (failedCover.nonEmpty)
      return Left(
        s"fractional cover fails at node ${failedCover.get._1} for vertex ${failedCover.get._2.name}"
      )
    Right(())
  }

  def toDot: String = {
    def escape(value: String): String = value.replace("\"", "\\\"")
    def formatWeight(weight: Double): String = {
      val rounded = math.rint(weight)
      if (math.abs(weight - rounded) < 1e-9) rounded.toLong.toString
      else f"$weight%.6g"
    }
    val builder = StringBuilder()
    builder ++= "digraph fractional_hypertree_decomposition {\n"
    builder ++= "  graph [rankdir=TB, bgcolor=\"white\", nodesep=0.35, ranksep=0.55];\n"
    builder ++= "  node [shape=box, style=\"rounded,filled\", fillcolor=\"#eef5ff\", " +
      "color=\"#4472a8\", fontname=\"Helvetica\", fontsize=10];\n"
    builder ++= "  edge [color=\"#6b7280\"];\n"
    nodes.sortBy(_.id).foreach { node =>
      val chi = node.chi.toVector.map(_.name).sorted.mkString("{", ", ", "}")
      val lambda = node.lambda.toVector.sortBy(_._1.alias).map { case (edge, weight) =>
        s"${edge.alias}:${formatWeight(weight)}"
      }.mkString("{", ", ", "}")
      val label = escape(s"χ = $chi\\nρ* = ${formatWeight(node.fractionalCoverNumber)}\\nλ = $lambda")
      builder ++= s"  n${node.id} [label=\"$label\"];\n"
      node.children.foreach(child => builder ++= s"  n${node.id} -> n${child.id};\n")
    }
    builder ++= "}\n"
    builder.result()
  }
}

/** Exact subset dynamic program for fractional hypertree width.
  *
  * Fractional hypertree width is the minimum, over tree decompositions of the
  * primal graph, of the maximum fractional edge-cover number of a bag.  The
  * dynamic program considers every elimination order.  For an eliminated set
  * S and next vertex v, the resulting bag is v together with every remaining
  * vertex reachable from v through S.
  *
  * This is the standard (not projection-free) fractional hypertree width.  A
  * projection-free integral hypertree width is therefore a valid upper bound,
  * but need not coincide with the integral analogue of this computation.
  */
class FractionalHypertreeWidth {

  private val Epsilon = 1e-10
  private case class FractionalCover(cost: Double, edgeWeights: Array[Double])
  private case class EliminationRecord(vertex: Int, chiMask: Int)

  def run(hypergraph: Hypergraph): Double = runWithDecomposition(hypergraph)._1

  def runWithDecomposition(
      hypergraph: Hypergraph
  ): (Double, FractionalHypertreeDecomposition) = {
    val vertices = hypergraph.vertices.toVector.sortBy(_.name)
    require(
      vertices.size < 31,
      s"Subset DP uses Int masks and supports at most 30 vertices, got ${vertices.size}"
    )

    val vertexIndex = vertices.zipWithIndex.toMap
    val edges = hypergraph.edges.toVector.sortBy(edge => (edge.alias, edge.tableName))
    val edgeMasks = edges
      .map(edge => maskOf(edge.nodes, vertexIndex))
      .toArray
    val adjacency = primalAdjacency(vertices.size, edgeMasks)
    val full = (1 << vertices.size) - 1
    val coverCache = mutable.Map(0 -> FractionalCover(0.0, Array.fill(edgeMasks.length)(0.0)))
    val dp = Array.fill(1 << vertices.size)(Double.PositiveInfinity)
    val previousState = Array.fill(1 << vertices.size)(-1)
    val previousVertex = Array.fill(1 << vertices.size)(-1)
    val previousBag = Array.fill(1 << vertices.size)(0)
    dp(0) = 0.0

    var eliminated = 0
    while (eliminated <= full) {
      var available = full ^ eliminated
      while (available != 0) {
        val vertexBit = available & -available
        available ^= vertexBit
        val vertex = Integer.numberOfTrailingZeros(vertexBit)
        val bag = eliminationBag(eliminated, vertex, adjacency, full)
        val cover = coverCache.getOrElseUpdate(
          bag,
          fractionalEdgeCover(bag, vertices.size, edgeMasks)
        )
        val next = eliminated | vertexBit
        val candidate = math.max(dp(eliminated), cover.cost)
        if (candidate + Epsilon < dp(next)) {
          dp(next) = candidate
          previousState(next) = eliminated
          previousVertex(next) = vertex
          previousBag(next) = bag
        }
      }
      eliminated += 1
    }

    val reversed = mutable.ArrayBuffer.empty[EliminationRecord]
    var state = full
    while (state != 0) {
      reversed += EliminationRecord(previousVertex(state), previousBag(state))
      state = previousState(state)
    }
    val records = reversed.reverse.toVector
    val position = Array.fill(vertices.size)(-1)
    records.zipWithIndex.foreach { case (record, index) => position(record.vertex) = index }
    val children = Array.fill(records.size)(Vector.empty[Int])
    val roots = mutable.ArrayBuffer.empty[Int]
    records.zipWithIndex.foreach { case (record, index) =>
      var higher = record.chiMask & ~(1 << record.vertex)
      if (higher == 0) roots += index
      else {
        var parent = -1
        while (higher != 0) {
          val bit = higher & -higher
          higher ^= bit
          val candidate = position(Integer.numberOfTrailingZeros(bit))
          if (parent == -1 || candidate < parent) parent = candidate
        }
        children(parent) :+= index
      }
    }
    roots.drop(1).foreach(root => children(roots.head) :+= root)
    val rootIndex = roots.head

    def build(index: Int): FractionalHypertreeDecompositionNode = {
      val record = records(index)
      val cover = coverCache(record.chiMask)
      val chi = vertices.indices
        .filter(vertex => (record.chiMask & (1 << vertex)) != 0)
        .map(vertices)
        .toSet
      val lambda = edgeMasks.indices.collect {
        case edge if cover.edgeWeights(edge) > Epsilon =>
          edges(edge) -> cover.edgeWeights(edge)
      }.toMap
      FractionalHypertreeDecompositionNode(index, chi, lambda, children(index).map(build))
    }

    val decomposition = FractionalHypertreeDecomposition(build(rootIndex))
    decomposition.validate(hypergraph) match {
      case Left(error) => throw new IllegalStateException(s"Constructed invalid FHD: $error")
      case Right(_) =>
    }
    if (math.abs(decomposition.width - dp(full)) > 1e-8)
      throw new IllegalStateException(
        s"Witness width ${decomposition.width} does not match optimum ${dp(full)}"
      )
    (dp(full), decomposition)
  }

  private def maskOf(vertices: Iterable[Vertex], index: Map[Vertex, Int]): Int =
    vertices.foldLeft(0)((mask, vertex) => mask | (1 << index(vertex)))

  private def primalAdjacency(numVertices: Int, edgeMasks: Array[Int]): Array[Int] = {
    val adjacency = Array.fill(numVertices)(0)
    edgeMasks.foreach { edge =>
      var remaining = edge
      while (remaining != 0) {
        val bit = remaining & -remaining
        val vertex = Integer.numberOfTrailingZeros(bit)
        adjacency(vertex) |= edge ^ bit
        remaining ^= bit
      }
    }
    adjacency
  }

  private def eliminationBag(
      eliminated: Int,
      vertex: Int,
      adjacency: Array[Int],
      full: Int
  ): Int = {
    val vertexBit = 1 << vertex
    var component = vertexBit
    var frontier = adjacency(vertex) & eliminated
    while (frontier != 0) {
      val bit = frontier & -frontier
      frontier ^= bit
      if ((component & bit) == 0) {
        component |= bit
        val member = Integer.numberOfTrailingZeros(bit)
        frontier |= adjacency(member) & eliminated & ~component
      }
    }

    var boundary = 0
    var remaining = component
    while (remaining != 0) {
      val bit = remaining & -remaining
      remaining ^= bit
      boundary |= adjacency(Integer.numberOfTrailingZeros(bit))
    }
    vertexBit | (boundary & (full ^ eliminated))
  }

  /**
    * Solve the dual fractional edge-cover LP with a primal simplex tableau:
    *
    *   max sum_{v in bag} y_v
    *   s.t. sum_{v in e} y_v <= 1  for every hyperedge e
    *        y_v >= 0.
    *
    * The all-slack basis is feasible, so no phase-I procedure is needed.
    */
  private def fractionalEdgeCover(
      bag: Int,
      numVertices: Int,
      edgeMasks: Array[Int]
  ): FractionalCover = {
    val numRows = edgeMasks.length
    val rhs = numVertices + numRows
    val objective = numRows
    val tableau = Array.fill(numRows + 1, rhs + 1)(0.0)

    var row = 0
    while (row < numRows) {
      var verticesInEdge = edgeMasks(row)
      while (verticesInEdge != 0) {
        val bit = verticesInEdge & -verticesInEdge
        tableau(row)(Integer.numberOfTrailingZeros(bit)) = 1.0
        verticesInEdge ^= bit
      }
      tableau(row)(numVertices + row) = 1.0
      tableau(row)(rhs) = 1.0
      row += 1
    }

    var vertex = 0
    while (vertex < numVertices) {
      if ((bag & (1 << vertex)) != 0) tableau(objective)(vertex) = -1.0
      vertex += 1
    }

    var optimal = false
    while (!optimal) {
      var entering = -1
      var column = 0
      while (column < rhs && entering == -1) {
        if (tableau(objective)(column) < -Epsilon) entering = column
        column += 1
      }

      if (entering == -1) {
        optimal = true
      } else {
        var leaving = -1
        var bestRatio = Double.PositiveInfinity
        row = 0
        while (row < numRows) {
          val coefficient = tableau(row)(entering)
          if (coefficient > Epsilon) {
            val ratio = tableau(row)(rhs) / coefficient
            if (ratio < bestRatio - Epsilon) {
              bestRatio = ratio
              leaving = row
            }
          }
          row += 1
        }
        if (leaving == -1)
          throw new IllegalArgumentException(
            "Unbounded fractional edge-cover dual; a bag vertex is in no hyperedge"
          )
        pivot(tableau, leaving, entering)
      }
    }

    val rawValue = tableau(objective)(rhs)
    val value = if (math.abs(rawValue - math.rint(rawValue)) < Epsilon) math.rint(rawValue) else rawValue
    val weights = Array.tabulate(numRows) { edge =>
      val weight = tableau(objective)(numVertices + edge)
      if (math.abs(weight) < Epsilon) 0.0 else weight
    }
    FractionalCover(value, weights)
  }

  private def pivot(tableau: Array[Array[Double]], pivotRow: Int, pivotColumn: Int): Unit = {
    val width = tableau(0).length
    val pivotValue = tableau(pivotRow)(pivotColumn)
    var column = 0
    while (column < width) {
      tableau(pivotRow)(column) /= pivotValue
      column += 1
    }

    var row = 0
    while (row < tableau.length) {
      if (row != pivotRow) {
        val factor = tableau(row)(pivotColumn)
        if (math.abs(factor) > Epsilon) {
          column = 0
          while (column < width) {
            tableau(row)(column) -= factor * tableau(pivotRow)(column)
            column += 1
          }
        }
      }
      row += 1
    }
  }
}
