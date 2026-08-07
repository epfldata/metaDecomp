package decompositions

import decompositions.Hypergraph.{Separator, Component}
import decompositions.MAA.DecompositionNode
import decompositions.NamedElement

object IncrementalMAA {

	private def canonicalKey[T <: NamedElement](s: Set[T]): String = s.toList.map(_.name).sorted.mkString(",")

	private def orderedCandidates(G: MetaDecompGraph, R: Separator, C_R: Component): Vector[Separator] =
		MAA.candidates(G, R, C_R).toVector.sortBy(canonicalKey)

	private def orderedChildComponents(G: MetaDecompGraph, S: Separator, C_R: Component): Vector[Component] =
		MAA.childComponents(G, S, C_R).toVector.sortBy(canonicalKey)


	private case class ExtendedNode(
		separator: Separator,
		component: Component,
		parentSeparator: Separator,
		pointerIndex: Int,
		children: Vector[ExtendedNode]
	) {
		def toDecompositionNode: DecompositionNode =
			DecompositionNode(separator, children.map(_.toDecompositionNode).toSet)
	}

	private def init(G: MetaDecompGraph, C_R: Component, R: Separator): ExtendedNode = {
		val candidateList = orderedCandidates(G, R, C_R)
		require(
			candidateList.nonEmpty,
			s"No separator available to cover component $C_R under parent separator $R; " +
				"this should not happen for a component reachable in a valid meta-decomposition."
		)
		val S = candidateList.head
		val children = orderedChildComponents(G, S, C_R).map(C => init(G, C, S))
		ExtendedNode(S, C_R, R, 0, children)
	}

	private def nextEHD(G: MetaDecompGraph, node: ExtendedNode): Option[ExtendedNode] = {
		val numChildren = node.children.length
		var i = numChildren - 1
		while (i >= 0) {
			nextEHD(G, node.children(i)) match {
				case Some(advancedChild) =>
					val laterSiblingsReset = ((i + 1) until numChildren).map(j => init(G, node.children(j).component, node.separator))
					return Some(node.copy(children = (node.children.take(i) :+ advancedChild) ++ laterSiblingsReset))
				case None =>
					i -= 1
			}
		}

		val candidateList = orderedCandidates(G, node.parentSeparator, node.component)
		val nextIndex = node.pointerIndex + 1
		if (nextIndex < candidateList.length) {
			val S = candidateList(nextIndex)
			val children = orderedChildComponents(G, S, node.component).map(C => init(G, C, S))
			Some(ExtendedNode(S, node.component, node.parentSeparator, nextIndex, children))
		} else {
			None
		}
	}

	def enumerate(H: Hypergraph, G: MetaDecompGraph): Iterator[DecompositionNode] = {
		if (MAA.candidates(G, Set.empty, H.vertices).isEmpty) Iterator.empty
		else new Iterator[DecompositionNode] {
			private var current: Option[ExtendedNode] = Some(init(G, H.vertices, Set.empty))

			override def hasNext: Boolean = current.isDefined

			override def next(): DecompositionNode = current match {
				case Some(node) =>
					val result = node.toDecompositionNode
					current = nextEHD(G, node)
					result
				case None =>
					Iterator.empty.next()
			}
		}
	}
}
