package decompositions

import decompositions.Hypergraph.{Vertex, Separator, Component, HyperedgeSetExtension}
import decompositions.MAA.DecompositionNode
import utils.subsetsOfSizeAtMost

import scala.collection.mutable

object PlainMAA {
	private[decompositions] def candidates(H: Hypergraph, R: Separator, C_R: Component, width: Int): Set[Separator] = {
		val nodesInROrC = C_R ++ R.nodes
		val edgesInComponent = H.edgesInComponent(C_R)

		H.edges.filter(_.nodes.subsetOf(nodesInROrC)).subsetsOfSizeAtMost(width).filter { S =>
			edgesInComponent.intersect(S).nonEmpty &&
				edgesInComponent.forall(_.nodes.intersect(R.nodes).subsetOf(S.nodes)) &&
				H.isConnected(R ++ S) &&
				H.isConnected(S -- childComponents(H, S, C_R).flatMap(H.edgesInComponent))
		}.toSet
	}

	private[decompositions] def childComponents(H: Hypergraph, S: Separator, C_R: Component): Set[Component] =
		H.componentsInducedBy(S.nodes).filter(_.subsetOf(C_R))

	def enumerate(H: Hypergraph, width: Int): Iterator[DecompositionNode] = {
		val memo = mutable.Map.empty[(Component, Separator), LazyList[DecompositionNode]]

		def kDecompositions(C_R: Component, R: Separator): LazyList[DecompositionNode] =
			memo.getOrElseUpdate((C_R, R), {
				candidates(H, R, C_R, width).to(LazyList).flatMap { S =>
					val components = childComponents(H, S, C_R).toList

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
}
