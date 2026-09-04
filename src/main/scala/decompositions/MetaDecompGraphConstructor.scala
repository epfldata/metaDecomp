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
				hypergraph.enumNextSeparatorCandidates(prevSep = r, component = c, width = width).foreach(s => graph.addEdge(r, s, c))
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
		val reachableVertices = mutable.Set.empty[Separator]
		val reachabilityQueue = mutable.Queue.empty[(Separator, Component)]
		reachabilityQueue.enqueue((root, hypergraph.vertices))
		while (reachabilityQueue.nonEmpty) {
			val (r, cr) = reachabilityQueue.dequeue()
			reachableVertices += r
			for ((cs, ss) <- graph.adjList(r) if (cs.subsetOf(cr)) ; s <- ss if (!reachableVertices.contains(s))) {
				reachabilityQueue.enqueue((s, cs))
			}
		}
		graph.vertices.filterNot(reachableVertices.contains).foreach(graph.removeVertex)
		return graph
	}
}
