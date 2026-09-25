package decompositions

import decompositions.Hypergraph.{Vertex, Hyperedge, Separator, Component}
import scala.collection.mutable
import utils.subsetsOfSizeAtMost
import decompositions.CostModel.getCumulativeCost

class MetaDecompCyclicOptimizer()(implicit sqlIR: sql.IR) {
	def optimizeLocalDP(subplans: Set[PlanNode]): PlanNode = {
		val dp = mutable.Map.empty[Set[PlanNode], PlanNode]
		Range(1, subplans.size + 1).foreach(size => {
			subplans.subsets(size).filter(s => sqlIR.cardinalities.contains(s.flatMap(p => p.allJoinedRelations))).foreach(subset => {
				subset.size match {
					case 1 => dp(subset) = subset.head
					case _ =>
						val (s1, s2) =
							subset.subsetsOfSizeAtMost(subset.size / 2)
								.map(s => (s, subset -- s))
								.filter((s1, s2) => s1.nonEmpty && s2.nonEmpty && dp.contains(s1) && dp.contains(s2))
								.minBy((s1, s2) => CostModel.getCumulativeCost(dp(s2), dp(s1)))
						val subplan1 = dp(s1)
						val subplan2 = dp(s2)
						dp(subset) = if subplan1.cardinality > subplan2.cardinality then new JoinNode(subplan1, subplan2) else new JoinNode(subplan2, subplan1)
				}
			})
		})
		dp(subplans)
	}

	def optimizeLocalHeuristic(subplans: Set[PlanNode]): PlanNode = {
		val partialPlans: mutable.Set[PlanNode] = mutable.Set.from(subplans)
		while (partialPlans.size > 1) {
			val (bestPair, bestPlan) =
				partialPlans
					.subsets(2)
					.map(_.toSeq)
					.filter(pair => pair.head.allJoinedRelations.nodes.intersect(pair.last.allJoinedRelations.nodes).nonEmpty)
					.map(pair => pair -> (if pair.head.cardinality > pair.last.cardinality || pair.head.isInstanceOf[ScanNode] && !pair.last.isInstanceOf[ScanNode] then new JoinNode(pair.head, pair.last) else new JoinNode(pair.last, pair.head)))
					.toList
					.sortWith { case ((pair1, plan1), (pair2, plan2)) =>
						if (plan1.cumulativeCost == plan2.cumulativeCost) plan1.cardinality < plan2.cardinality else plan1.cumulativeCost < plan2.cumulativeCost
					}
					.head
			partialPlans --= bestPair
			partialPlans += bestPlan
		}
		partialPlans.head

		// val remaining = mutable.Set.from(subplans)
		// var plan = subplans.minBy(_.cardinality)
		// remaining -= plan
		// while (remaining.nonEmpty) {
		// 	val next = remaining
		// 		.filter(_.allJoinedRelations.flatMap(_.nodes).intersect(plan.allJoinedRelations.flatMap(_.nodes)).nonEmpty)
		// 		.toList
		// 		.sortWith((a, b) => {
		// 			val costWithA = getCumulativeCost(a, plan)
		// 			val costWithB = getCumulativeCost(b, plan)
		// 			if (costWithA == costWithB) a.cardinality < b.cardinality else costWithA < costWithB
		// 		})
		// 		.head
		// 	plan = if plan.cardinality > next.cardinality || next.isInstanceOf[ScanNode] && !plan.isInstanceOf[ScanNode] then JoinNode(plan, next) else JoinNode(next, plan)
		// 	remaining -= next
		// }
		// plan
	}

	def optimizeLocal(subplans: Set[PlanNode]): PlanNode =
		if subplans.size >= 10 then optimizeLocalHeuristic(subplans) else optimizeLocalDP(subplans)

	private def computePlans(hypergraph: Hypergraph, metaDecompGraph: MetaDecompGraph): (
		mutable.Map[(Separator, Separator), PlanNode],
		mutable.Map[(Component, Separator), Separator]
	) = {
		val edgeToPlan = mutable.Map.empty[(Separator, Separator), PlanNode]
		// Child edges precede their parents in sortedEdges, so all alternatives are
		// finalized on first use. Keep this cache local to this computation.
		val componentsBestEdge = mutable.Map.empty[(Component, Separator), Separator]
		metaDecompGraph.sortedEdges.foreach((r, s, crs) => {
			val componentBestEdge = metaDecompGraph.adjList(s).keySet.toSet.filter(_.subsetOf(crs)).map(cst => cst -> componentsBestEdge.getOrElseUpdate(
				(cst, s),
				metaDecompGraph.adjList(s)(cst).minBy(t => edgeToPlan((s, t)).cumulativeCost)
			))
			val childrenPlans = componentBestEdge.map((_, t) => edgeToPlan((s, t)))
			val newRelations = hypergraph.edges.filter(_.nodes.subsetOf(s.nodes)) -- childrenPlans.flatMap(_.allJoinedRelations)
			try {
				val allSubplans = childrenPlans ++ newRelations.map(ScanNode(_)) // if newRelations.isEmpty then childrenPlans else childrenPlans + optimizeLocal(newRelations.map(ScanNode(_)).toSet)
				edgeToPlan((r, s)) = optimizeLocal(allSubplans)
			} catch {
				case _: Throwable => {
					println(s"${r.asString} -> ${s.asString}, new relations: ${newRelations.asString}")
					println(s"Components of ${s.asString}:")
					println(metaDecompGraph.adjList(s).keySet.toSet.filter(_.subsetOf(crs)).map(c => s"${c.asString}, next separator: ${metaDecompGraph.adjList(s)(c).minBy(t => edgeToPlan((s, t)).cumulativeCost).asString}").mkString("\n"))
					while (true) {}
				}
			}

		})
		(edgeToPlan, componentsBestEdge)
	}

	def run(hypergraph: Hypergraph, metaDecompGraph: MetaDecompGraph): PlanNode = {
		val edgeToPlan = mutable.Map.empty[(Separator, Separator), PlanNode]
		// Cache only after the bottom-up traversal has finalized the child plans.
		val componentBestPlan = mutable.Map.empty[(Separator, Component), PlanNode]
		metaDecompGraph.sortedEdges.foreach((r, s, crs) => {
			val childrenPlans = metaDecompGraph.adjList(s).keySet.toSet.filter(_.subsetOf(crs)).map(cst =>
				componentBestPlan.getOrElseUpdate((s, cst),
					// Preserve selection over the mapped plan set: selecting a separator
					// first can change which plan wins an equal-cost tie.
					metaDecompGraph.adjList(s)(cst).map(t => edgeToPlan((s, t))).minBy(_.cumulativeCost)
				)
			)
			val newRelations = hypergraph.edges.filter(_.nodes.subsetOf(s.nodes)) -- childrenPlans.flatMap(_.allJoinedRelations)
			try {
				val allSubplans = childrenPlans ++ newRelations.map(ScanNode(_)) // if newRelations.isEmpty then childrenPlans else childrenPlans + optimizeLocal(newRelations.map(ScanNode(_)).toSet)
				edgeToPlan((r, s)) = optimizeLocal(allSubplans)
			} catch {
				case _: Throwable => {
					println(s"${r.asString} -> ${s.asString}, new relations: ${newRelations.asString}")
					println(s"Components of ${s.asString}:")
					println(metaDecompGraph.adjList(s).keySet.toSet.filter(_.subsetOf(crs)).map(c => s"${c.asString}, next separator: ${metaDecompGraph.adjList(s)(c).minBy(t => edgeToPlan((s, t)).cumulativeCost).asString}").mkString("\n"))
					while (true) {}
				}
			}
		})
		val root = metaDecompGraph.vertices.find(_.size == 0).get
		// val first = metaDecompGraph.adjList(root).flatMap(_._2).minBy(t => edgeToPlan(root, t).cumulativeCost)
		// println(s"First separator: ${first.asString}")
		val minPlan = metaDecompGraph.adjList(root).flatMap(_._2).map(t => edgeToPlan(root, t)).minBy(_.cumulativeCost)
		minPlan.projectTo = sqlIR.outputAttributes
		minPlan
	}

	def runWithoutMemo(hypergraph: Hypergraph, metaDecompGraph: MetaDecompGraph): PlanNode = {
		val edgeToPlan = mutable.Map.empty[(Separator, Separator), PlanNode]
		metaDecompGraph.sortedEdges.foreach((r, s, crs) => {
			val componentBestEdge = metaDecompGraph.adjList(s).keySet.toSet.filter(_.subsetOf(crs)).map(cst => cst -> metaDecompGraph.adjList(s)(cst).minBy(t => edgeToPlan((s, t)).cumulativeCost))
			val childrenPlans = metaDecompGraph.adjList(s).keySet.toSet.filter(_.subsetOf(crs)).map(cst => metaDecompGraph.adjList(s)(cst).map(t => edgeToPlan((s, t))).minBy(_.cumulativeCost))
			val newRelations = hypergraph.edges.filter(_.nodes.subsetOf(s.nodes)) -- childrenPlans.flatMap(_.allJoinedRelations)
			try {
				val allSubplans = childrenPlans ++ newRelations.map(ScanNode(_)) // if newRelations.isEmpty then childrenPlans else childrenPlans + optimizeLocal(newRelations.map(ScanNode(_)).toSet)
				edgeToPlan((r, s)) = optimizeLocal(allSubplans)
			} catch {
				case _: Throwable => {
					println(s"${r.asString} -> ${s.asString}, new relations: ${newRelations.asString}")
					println(s"Components of ${s.asString}:")
					println(metaDecompGraph.adjList(s).keySet.toSet.filter(_.subsetOf(crs)).map(c => s"${c.asString}, next separator: ${metaDecompGraph.adjList(s)(c).minBy(t => edgeToPlan((s, t)).cumulativeCost).asString}").mkString("\n"))
					while (true) {}
				}
			}
			// println(s"plan for ($r -> $s)")
			// println(dp(elements))

			// println(s"At edge ($r -> $s)")
			// println(s"  Children: ")
			// println(s"  ${componentBestEdge.map(p => s"(${p._1.asString}) -> (${p._2.asString})\n${edgeToPlan((s, p._2))}").mkString(" ; ")}")
			// println(s"  Optimal query plan: ${edgeToPlan((r, s))}")
		})
		val root = metaDecompGraph.vertices.find(_.size == 0).get
		// val first = metaDecompGraph.adjList(root).flatMap(_._2).minBy(t => edgeToPlan(root, t).cumulativeCost)
		// println(s"First separator: ${first.asString}")
		val minPlan = metaDecompGraph.adjList(root).flatMap(_._2).map(t => edgeToPlan(root, t)).minBy(_.cumulativeCost)
		minPlan.projectTo = sqlIR.outputAttributes
		minPlan
	}

	/** Returns a new graph containing only the edges inducing a minimum-cost plan. */
	def runOptimalDecomposition(hypergraph: Hypergraph, metaDecompGraph: MetaDecompGraph): (PlanNode, MetaDecompGraph) = {
		val (edgeToPlan, componentsBestEdge) = computePlans(hypergraph, metaDecompGraph)
		val root = metaDecompGraph.vertices.find(_.isEmpty).get
		val minPlan = metaDecompGraph.adjList(root).flatMap(_._2).map(t => edgeToPlan(root, t)).minBy(_.cumulativeCost)
		minPlan.projectTo = sqlIR.outputAttributes

		val first = metaDecompGraph.adjList(root).flatMap(_._2).minBy(t => edgeToPlan((root, t)).cumulativeCost)
		val optimalGraph = new MetaDecompGraph

		def restore(r: Separator, s: Separator, component: Component): Unit = {
			if (!optimalGraph.sortedEdges.contains((r, s, component))) {
				optimalGraph.addEdge(r, s, component)
				metaDecompGraph.adjList(s).keysIterator.filter(_.subsetOf(component)).foreach { c =>
					restore(s, componentsBestEdge((c, s)), c)
				}
			}
		}

		restore(root, first, hypergraph.vertices)
		(minPlan, optimalGraph)
	}
}
