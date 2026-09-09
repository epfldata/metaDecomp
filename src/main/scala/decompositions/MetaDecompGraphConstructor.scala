package decompositions

import sql.IR
import decompositions.Hypergraph.{Vertex, Hyperedge, HyperedgeSetExtension, Separator, Component}
import decompositions.MetaDecompGraph
import utils._
import scala.collection.mutable

class MetaDecompGraphConstructor {

	def run(hypergraph: Hypergraph, width: Int): MetaDecompGraph = {
		val graph = new MetaDecompGraph()

		for (r <- hypergraph.edges.subsetsOfSizeAtMost(width)) {
			graph.vertices += r
			graph.adjList(r) = mutable.Map.empty
			for (c <- hypergraph.componentsInducedBy(r.nodes)) {
				graph.adjList(r)(c) = mutable.Set.empty
				val nodesInROrC = c ++ r.nodes
				for (s <- hypergraph.edges.filter(_.nodes.subsetOf(nodesInROrC)).subsetsOfSizeAtMost(width)
					if (hypergraph.edgesInComponent(c).intersect(s).nonEmpty
						&& hypergraph.edgesInComponent(c).forall(_.nodes.intersect(r.nodes).subsetOf(s.nodes)
						&& hypergraph.isConnected(r ++ s)
						&& hypergraph.isConnected(s ++ hypergraph.edgesInComponent(c))
						)
					)) {
						graph.addEdge(r, s, c)
				}
			}
		}


		val checkQueue = mutable.Queue.from(graph.sortedEdges)
		while (checkQueue.nonEmpty) {
			val (r, s, crs) = checkQueue.dequeue()
			if (graph.adjList(s).exists { case (cst, ts) => cst.subsetOf(crs) && ts.isEmpty }) {
				graph.removeEdge(r, s, crs)
			}
		}


		val root = graph.vertices.find(_.isEmpty).get
		val reachableSeparators = mutable.Set.empty[Separator]
		val visitedStates =
			mutable.Set.empty[(Separator, Component)]

		val reachabilityQueue =
			mutable.Queue.empty[(Separator, Component)]

		reachabilityQueue.enqueue((root, hypergraph.vertices))

		while (reachabilityQueue.nonEmpty) {
			val state@(r, cr) = reachabilityQueue.dequeue()

			if (visitedStates.add(state)) {
				reachableSeparators += r

				for {
					(cs, successors) <- graph.adjList.getOrElse(
						r,
						mutable.Map.empty[Component, mutable.Set[Separator]]
					)
					if cs.subsetOf(cr)
					s <- successors
				} {
					reachabilityQueue.enqueue((s, cs))
				}
			}
		}

		graph.vertices
			.filterNot(reachableSeparators.contains)
			.foreach(graph.removeVertex)

		/*graph.sortedEdges.foreach(
			(r, s, crs) => {
				for (c <- hypergraph.componentsInducedBy(s.nodes)) {
					if (c.subsetOf(crs) && graph.adjList(s)(c).isEmpty) {
						println(s"Local condition failed")
					}
				}
			}
		)*/
		return graph
	}
}
