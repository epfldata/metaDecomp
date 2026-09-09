package decompositions

import decompositions.Hypergraph.{Hyperedge, Separator, Vertex}

import scala.collection.mutable
import utils.subsetsOfSizeAtMost
import decompositions.CostModel.getCumulativeCost

import java.awt.Component

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
		if subplans.size >= 8 then optimizeLocalHeuristic(subplans) else optimizeLocalDP(subplans)

	def run(hypergraph: Hypergraph, metaDecompGraph: MetaDecompGraph): PlanNode = {
		val edgeToPlan = mutable.Map.empty[(Separator, Separator), PlanNode]
		var bestEdges: mutable.Map[(Separator, Separator, Hypergraph.Component),Set[(Hypergraph.Component, Separator)]] = mutable.Map.empty
		metaDecompGraph.sortedEdges.foreach((r, s, crs) => {
			val componentBestEdge: Set[(Hypergraph.Component, Separator)] = metaDecompGraph.adjList(s).keySet.toSet.filter(_.subsetOf(crs)).map(cst => cst -> metaDecompGraph.adjList(s)(cst).minBy(t => edgeToPlan((s, t)).cumulativeCost))
			bestEdges((r, s, crs)) = componentBestEdge
			//println(s"Best edges for ${(r, s, crs)}: $componentBestEdge")
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
		def printHD(r: Separator, s: Separator, crs: Hypergraph.Component): Unit = {
			println(s"($r, $s, $crs)")
			bestEdges((r, s, crs)).foreach((cst, t) => printHD(s, t, cst))
		}

		val root = metaDecompGraph.vertices.find(_.size == 0).get
		//val first = metaDecompGraph.adjList(root).flatMap(_._2).minBy(t => edgeToPlan(root, t).cumulativeCost)
		//println(s"First separator: ${first.asString}")
		//printHD(root, first, hypergraph.vertices)
		val minPlan = metaDecompGraph.adjList(root).flatMap(_._2).map(t => edgeToPlan(root, t)).minBy(_.cumulativeCost)
		minPlan.projectTo = sqlIR.outputAttributes
		minPlan
	}
}
