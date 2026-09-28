package decompositions

import decompositions.Hypergraph.{Hyperedge, HyperedgeSetExtension, Vertex}

import scala.collection.mutable

case class RegularHypertreeDecompositionNode(
    id: Int,
    chi: Set[Vertex],
    lambda: Set[Hyperedge],
    children: Vector[RegularHypertreeDecompositionNode]
)

case class RegularHypertreeDecomposition(root: RegularHypertreeDecompositionNode) {
  lazy val nodes: Vector[RegularHypertreeDecompositionNode] = {
    def collect(node: RegularHypertreeDecompositionNode): Vector[RegularHypertreeDecompositionNode] =
      node +: node.children.flatMap(collect)
    collect(root)
  }

  def width: Int = nodes.map(_.lambda.size).max

  def nonProjectionFreeNodes: Vector[RegularHypertreeDecompositionNode] =
    nodes.filter(node => node.chi != node.lambda.nodes)

  def isProjectionFree: Boolean = nonProjectionFreeNodes.isEmpty

  /** Validate all four conditions of a regular hypertree decomposition. */
  def validate(hypergraph: Hypergraph): Either[String, Unit] = {
    val byId = nodes.map(node => node.id -> node).toMap
    if (byId.size != nodes.size) return Left("decomposition node IDs are not unique")

    val treeAdjacency = mutable.Map.empty[Int, mutable.Set[Int]]
    nodes.foreach(node => treeAdjacency.getOrElseUpdate(node.id, mutable.Set.empty))
    nodes.foreach { node =>
      node.children.foreach { child =>
        treeAdjacency(node.id) += child.id
        treeAdjacency(child.id) += node.id
      }
    }

    val uncoveredEdges = hypergraph.edges.filterNot(edge => nodes.exists(node => edge.nodes.subsetOf(node.chi)))
    if (uncoveredEdges.nonEmpty)
      return Left(s"query hyperedges are not covered: ${uncoveredEdges.map(_.alias).mkString(",")}")

    val disconnectedVertex = hypergraph.vertices.find { vertex =>
      val containing = nodes.filter(_.chi.contains(vertex)).map(_.id).toSet
      if (containing.nonEmpty) {
        val visited = mutable.Set(containing.head)
        val queue = mutable.Queue(containing.head)
        while (queue.nonEmpty) {
          val current = queue.dequeue()
          treeAdjacency(current).filter(containing.contains).filterNot(visited.contains).foreach { next =>
            visited += next
            queue.enqueue(next)
          }
        }
        visited.toSet != containing
      } else false
    }
    if (disconnectedVertex.nonEmpty)
      return Left(s"running-intersection condition fails for vertex ${disconnectedVertex.get.name}")

    val uncoveredChi = nodes.find(node => !node.chi.subsetOf(node.lambda.nodes))
    if (uncoveredChi.nonEmpty)
      return Left(s"chi is not covered by lambda at node ${uncoveredChi.get.id}")

    def validateSpecialCondition(node: RegularHypertreeDecompositionNode): Either[String, Set[Vertex]] = {
      val childResults = node.children.map(validateSpecialCondition)
      childResults.collectFirst { case Left(error) => Left(error) } match {
        case Some(error) => error
        case None =>
          val descendantVertices = node.chi ++ childResults.collect { case Right(vertices) => vertices }.flatten
          val forbidden = node.lambda.nodes.intersect(descendantVertices) -- node.chi
          if (forbidden.nonEmpty)
            Left(
              s"special condition fails at node ${node.id} for vertices " +
                forbidden.map(_.name).toVector.sorted.mkString(",")
            )
          else Right(descendantVertices)
      }
    }

    validateSpecialCondition(root).map(_ => ())
  }
}

/** Construct a regular (non-projection-free) hypertree decomposition.
  *
  * The search first finds a tree decomposition whose bags have integral edge
  * covers of size at most `maxWidth`.  It then tries every rooting and chooses
  * lambda labels satisfying the hypertree special condition.  A returned
  * witness is independently validated by [[RegularHypertreeDecomposition.validate]].
  */
class RegularHypertreeDecompositionFinder {

  private case class EliminationRecord(vertex: Int, chiMask: Int)
  private case class Draft(
      chi: Set[Vertex],
      lambda: Set[Hyperedge],
      children: Vector[Draft]
  )

  def find(hypergraph: Hypergraph, maxWidth: Int): Option[RegularHypertreeDecomposition] = {
    require(maxWidth >= 1)
    val vertices = hypergraph.vertices.toVector.sortBy(_.name)
    require(vertices.size < 31, s"Subset DP supports at most 30 vertices, got ${vertices.size}")
    val vertexIndex = vertices.zipWithIndex.toMap
    val edges = hypergraph.edges.toVector.sortBy(edge => (edge.alias, edge.tableName))
    val edgeMasks = edges.map(edge => maskOf(edge.nodes, vertexIndex)).toArray
    val adjacency = primalAdjacency(vertices.size, edgeMasks)
    val full = (1 << vertices.size) - 1
    val coverCache = mutable.Map.empty[Int, Vector[Vector[Int]]]

    def covers(bag: Int): Vector[Vector[Int]] = coverCache.getOrElseUpdate(
      bag,
      coverOptions(bag, edgeMasks, maxWidth)
    )

    val reachable = Array.fill(1 << vertices.size)(false)
    val previousState = Array.fill(1 << vertices.size)(-1)
    val previousVertex = Array.fill(1 << vertices.size)(-1)
    val previousBag = Array.fill(1 << vertices.size)(0)
    reachable(0) = true

    var eliminated = 0
    while (eliminated <= full) {
      if (reachable(eliminated)) {
        var available = full ^ eliminated
        while (available != 0) {
          val vertexBit = available & -available
          available ^= vertexBit
          val vertex = Integer.numberOfTrailingZeros(vertexBit)
          val bag = eliminationBag(eliminated, vertex, adjacency, full)
          val next = eliminated | vertexBit
          if (!reachable(next) && covers(bag).nonEmpty) {
            reachable(next) = true
            previousState(next) = eliminated
            previousVertex(next) = vertex
            previousBag(next) = bag
          }
        }
      }
      eliminated += 1
    }
    if (!reachable(full)) return None

    val reversed = mutable.ArrayBuffer.empty[EliminationRecord]
    var state = full
    while (state != 0) {
      reversed += EliminationRecord(previousVertex(state), previousBag(state))
      state = previousState(state)
    }
    val records = reversed.reverse.toVector
    val position = Array.fill(vertices.size)(-1)
    records.zipWithIndex.foreach { case (record, index) => position(record.vertex) = index }

    val undirected = Array.fill(records.size)(mutable.Set.empty[Int])
    val componentRoots = mutable.ArrayBuffer.empty[Int]
    records.zipWithIndex.foreach { case (record, index) =>
      val higher = record.chiMask & ~(1 << record.vertex)
      if (higher == 0) componentRoots += index
      else {
        var remaining = higher
        var parent = -1
        while (remaining != 0) {
          val bit = remaining & -remaining
          remaining ^= bit
          val candidate = position(Integer.numberOfTrailingZeros(bit))
          if (parent == -1 || candidate < parent) parent = candidate
        }
        undirected(index) += parent
        undirected(parent) += index
      }
    }
    componentRoots.drop(1).foreach { root =>
      undirected(componentRoots.head) += root
      undirected(root) += componentRoots.head
    }

    val eliminationWitness = records.indices.iterator.flatMap { rootIndex =>
      orientAndLabel(rootIndex, records, undirected, covers, vertices, edges, edgeMasks, hypergraph)
    }.nextOption()
    eliminationWitness.orElse(findNormalForm(hypergraph, maxWidth))
  }

  /** Direct normal-form search for cases where the first integral-cover
    * elimination witness cannot be labelled to satisfy the special condition.
    * A child chi label consists of its parent interface plus the part of its
    * lambda union inside the current component; vertices outside are projected
    * away.
    */
  private def findNormalForm(
      hypergraph: Hypergraph,
      maxWidth: Int
  ): Option[RegularHypertreeDecomposition] = {
    val edges = hypergraph.edges.toVector.sortBy(edge => (edge.alias, edge.tableName))
    val guardSets = edgeSubsets(edges, maxWidth)
    val failed = mutable.Set.empty[(Set[Vertex], Set[Vertex])]

    def solve(component: Set[Vertex], parentChi: Set[Vertex]): Option[Draft] = {
      val state = component -> parentChi
      if (failed.contains(state)) return None
      val edgesInComponent = hypergraph.edges.filter(_.nodes.exists(component.contains))
      val interface = edgesInComponent.nodes.intersect(parentChi)

      guardSets.iterator.flatMap { lambda =>
        val lambdaNodes = lambda.nodes
        val chi = lambdaNodes.intersect(component) ++ interface
        if (
          lambda.intersect(edgesInComponent).isEmpty ||
          !interface.subsetOf(lambdaNodes) ||
          chi.intersect(component).isEmpty
        ) Iterator.empty
        else {
          val children = hypergraph.componentsInducedBy(chi, component).toVector
          if (children.exists(_.size >= component.size)) Iterator.empty
          else {
            val childDrafts = children.map(child => solve(child, chi))
            if (childDrafts.forall(_.nonEmpty))
              Iterator.single(Draft(chi, lambda, childDrafts.flatten))
            else Iterator.empty
          }
        }
      }.nextOption() match {
        case result @ Some(_) => result
        case None =>
          failed += state
          None
      }
    }

    val draft = guardSets.iterator.flatMap { lambda =>
      val chi = lambda.nodes
      val components = hypergraph.componentsInducedBy(chi).toVector
      val children = components.map(component => solve(component, chi))
      if (children.forall(_.nonEmpty)) Iterator.single(Draft(chi, lambda, children.flatten))
      else Iterator.empty
    }.nextOption()

    draft.flatMap { rootDraft =>
      var nextId = 0
      def build(current: Draft): RegularHypertreeDecompositionNode = {
        val id = nextId
        nextId += 1
        RegularHypertreeDecompositionNode(id, current.chi, current.lambda, current.children.map(build))
      }
      val decomposition = RegularHypertreeDecomposition(build(rootDraft))
      decomposition.validate(hypergraph).toOption.map(_ => decomposition)
    }
  }

  private def edgeSubsets(edges: Vector[Hyperedge], maxWidth: Int): Vector[Set[Hyperedge]] = {
    val result = mutable.ArrayBuffer.empty[Set[Hyperedge]]
    def recurse(start: Int, selected: Vector[Hyperedge]): Unit = {
      if (selected.nonEmpty) result += selected.toSet
      if (selected.size < maxWidth) {
        var edge = start
        while (edge < edges.size) {
          recurse(edge + 1, selected :+ edges(edge))
          edge += 1
        }
      }
    }
    recurse(0, Vector.empty)
    result.toVector
  }

  private def orientAndLabel(
      rootIndex: Int,
      records: Vector[EliminationRecord],
      undirected: Array[mutable.Set[Int]],
      covers: Int => Vector[Vector[Int]],
      vertices: Vector[Vertex],
      edges: Vector[Hyperedge],
      edgeMasks: Array[Int],
      hypergraph: Hypergraph
  ): Option[RegularHypertreeDecomposition] = {
    val parent = Array.fill(records.size)(-2)
    val children = Array.fill(records.size)(Vector.empty[Int])
    parent(rootIndex) = -1
    val queue = mutable.Queue(rootIndex)
    while (queue.nonEmpty) {
      val node = queue.dequeue()
      undirected(node).filter(_ != parent(node)).foreach { child =>
        parent(child) = node
        children(node) :+= child
        queue.enqueue(child)
      }
    }

    val descendantMasks = Array.fill(records.size)(0)
    def descendants(node: Int): Int = {
      val result = children(node).foldLeft(records(node).chiMask)((mask, child) => mask | descendants(child))
      descendantMasks(node) = result
      result
    }
    descendants(rootIndex)

    val chosenCovers = Array.fill[Vector[Int]](records.size)(Vector.empty)
    val labelsExist = records.indices.forall { node =>
      covers(records(node).chiMask).find { option =>
        val lambdaMask = option.foldLeft(0)((mask, edge) => mask | edgeMasks(edge))
        (lambdaMask & descendantMasks(node) & ~records(node).chiMask) == 0
      } match {
        case Some(option) =>
          chosenCovers(node) = option
          true
        case None => false
      }
    }
    if (!labelsExist) return None

    def build(node: Int): RegularHypertreeDecompositionNode =
      RegularHypertreeDecompositionNode(
        node,
        vertices.indices.filter(index => (records(node).chiMask & (1 << index)) != 0).map(vertices).toSet,
        chosenCovers(node).map(edges).toSet,
        children(node).map(build)
      )

    val decomposition = RegularHypertreeDecomposition(build(rootIndex))
    decomposition.validate(hypergraph) match {
      case Right(_) => Some(decomposition)
      case Left(error) => throw new IllegalStateException(s"Constructed invalid decomposition: $error")
    }
  }

  private def coverOptions(bag: Int, edgeMasks: Array[Int], maxWidth: Int): Vector[Vector[Int]] = {
    val result = mutable.ArrayBuffer.empty[Vector[Int]]
    def recurse(start: Int, selected: Vector[Int], union: Int): Unit = {
      if ((bag & ~union) == 0) result += selected
      else if (selected.size < maxWidth) {
        var edge = start
        while (edge < edgeMasks.length) {
          recurse(edge + 1, selected :+ edge, union | edgeMasks(edge))
          edge += 1
        }
      }
    }
    recurse(0, Vector.empty, 0)
    result.toVector
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

  private def eliminationBag(eliminated: Int, vertex: Int, adjacency: Array[Int], full: Int): Int = {
    val vertexBit = 1 << vertex
    var component = vertexBit
    var frontier = adjacency(vertex) & eliminated
    while (frontier != 0) {
      val bit = frontier & -frontier
      frontier ^= bit
      if ((component & bit) == 0) {
        component |= bit
        frontier |= adjacency(Integer.numberOfTrailingZeros(bit)) & eliminated & ~component
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
}
