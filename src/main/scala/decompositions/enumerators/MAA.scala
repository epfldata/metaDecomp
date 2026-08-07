package decompositions

import decompositions.Hypergraph.{Vertex, Separator, Component, HyperedgeSetExtension}

import scala.collection.mutable

object MAA {
	case class DecompositionNode(bag: Separator, children: Set[DecompositionNode]) {
		def width: Int = children.foldLeft(bag.size)((maxSoFar, child) => maxSoFar.max(child.width))
	}

	private[decompositions] def candidates(G: MetaDecompGraph, R: Separator, C_R: Component): Set[Separator] =
		G.adjList.get(R).flatMap(_.get(C_R)).map(_.toSet).getOrElse(Set.empty)

	private[decompositions] def childComponents(G: MetaDecompGraph, S: Separator, C_R: Component): Set[Component] =
		G.adjList.get(S).map(_.keySet.filter(_.subsetOf(C_R)).toSet).getOrElse(Set.empty)

	def accept(H: Hypergraph, G: MetaDecompGraph): Boolean = {
		val memo = mutable.Map.empty[(Component, Separator), Boolean]

		def kDecomposable(C_R: Component, R: Separator): Boolean =
			memo.getOrElseUpdate((C_R, R), {
				val m_R_of_CR = candidates(G, R, C_R)

				if m_R_of_CR.isEmpty then
					false
				else
					m_R_of_CR.exists { S =>
						childComponents(G, S, C_R).forall(C => kDecomposable(C, S))
					}
			})

		kDecomposable(H.vertices, Set.empty)
	}

	def enumerate(H: Hypergraph, G: MetaDecompGraph): Iterator[DecompositionNode] = {
		val memo = mutable.Map.empty[(Component, Separator), LazyList[DecompositionNode]]

		def kDecompositions(C_R: Component, R: Separator): LazyList[DecompositionNode] =
			memo.getOrElseUpdate((C_R, R), {
				candidates(G, R, C_R).to(LazyList).flatMap { S =>
					val components = childComponents(G, S, C_R).toList

					val perComponent = components.map(C => kDecompositions(C, S))
					if perComponent.exists(_.isEmpty) then
						LazyList.empty[DecompositionNode]
					else
						cartesianProduct(perComponent).map(children => DecompositionNode(S, children.toSet))
				}
			})

		kDecompositions(H.vertices, Set.empty).iterator
	}

	private def cartesianProduct[T](lists: List[LazyList[T]]): LazyList[List[T]] =
		lists.foldRight(LazyList(List.empty[T])) { (list, acc) =>
			for (x <- list; combo <- acc) yield x :: combo
		}


	def validate(H: Hypergraph, decomposition: DecompositionNode): Unit = {
		def verticesTouched(node: DecompositionNode): Set[Vertex] = {
			val childTouched = node.children.toList.map(verticesTouched)
			val ownVars = node.bag.nodes

			val sharedAcrossChildren = childTouched.flatten
				.groupBy(identity)
				.collect { case (v, occurrences) if occurrences.size >= 2 => v }
				.toSet

			val disconnected = sharedAcrossChildren -- ownVars
			if disconnected.nonEmpty then
				throw new IllegalArgumentException(
					s"Invalid hypertree decomposition: variable(s) ${disconnected.mkString(", ")} occur in more " +
						s"than one child subtree of bag ${node.bag.toList.map(_.alias).sorted.mkString("{", ",", "}")} " +
						"without being in that bag, violating connectedness (the running-intersection property)."
				)

			ownVars ++ childTouched.flatten
		}

		val touched = verticesTouched(decomposition)

		def allBagVars(node: DecompositionNode): List[Set[Vertex]] =
			node.bag.nodes :: node.children.iterator.flatMap(allBagVars).toList
		val bagVars = allBagVars(decomposition)

		val uncoveredEdges = H.edges.filterNot(e => bagVars.exists(chi => e.nodes.subsetOf(chi)))
		if uncoveredEdges.nonEmpty then
			throw new IllegalArgumentException(
				s"Invalid hypertree decomposition: hyperedge(s) ${uncoveredEdges.map(_.alias).mkString(", ")} " +
					"are not covered by any single bag."
			)

		val missingVertices = H.vertices -- touched
		if missingVertices.nonEmpty then
			throw new IllegalArgumentException(
				s"Invalid hypertree decomposition: vertex(vertices) ${missingVertices.mkString(", ")} do not appear in any bag."
			)
	}
}
