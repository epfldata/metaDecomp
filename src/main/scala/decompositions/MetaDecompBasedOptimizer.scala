package decompositions

import decompositions.CostModel.getCumulativeCost
import sql.{Attribute, Relation}

import scala.annotation.tailrec
import scala.collection.mutable

class MetaDecompBasedOptimizer()(implicit sqlIR: sql.IR) {
	def run(metaJoinTree: MetaNode[Attribute, Relation]): PlanNode = {
		planBottomUp(metaJoinTree)
		planTopDown(metaJoinTree, null)
		metaJoinTree.computeTopoOrder()
		val allNodes = metaJoinTree.collectDescendents.filter(_.isInstanceOf[MetaNodePhysical[Attribute, Relation]])
		allNodes.foreach(node => {
			val children = node match {
				case _: MetaNodePhysical[Attribute, Relation] => node.children
				case minorNode: MetaNodeMinor[Attribute, Relation] => minorNode.children ++ minorNode.origin
			}
			val neighbours = children ++ node.parent
			node.planWithCurrentAsRoot =
				if neighbours.isEmpty then
					new ScanNode(node.asInstanceOf[MetaNodePhysical[Attribute, Relation]].originalHyperEdge) {
						joinTree = TreeNode(node, Set.empty)
					}
				else neighbours
					.map(neighbour => new JoinNode(node.planFromNeighbour(neighbour), neighbour.planFromNeighbour(node)) {
						joinTree = TreeNode(node, neighbour.planFromNeighbour(node).joinTree.children + node.planFromNeighbour(neighbour).joinTree)
					})
					.minBy(_.cumulativeCost)
		})
		val optPlan = allNodes
			.filter(_.nodes.map(_.name)
				.intersect(
					sqlIR.outputAttributes
						.map(_.qualifiedCol).map(qualifiedCol => {
							sqlIR.vertexIdToColumnName.find { case (id, map) => map.exists(_ == qualifiedCol.hyperedge && _ == qualifiedCol.column) }.head._1
						})
				)
				.nonEmpty
			)
			.toSeq
			.sortBy(_.order)
			.map(_.planWithCurrentAsRoot)
			.minBy(_.cumulativeCost)
		optPlan.projectTo = sqlIR.outputAttributes
		optPlan
	}

	def planBottomUp(metaNode: MetaNode[Attribute, Relation]): PlanNode = {
		val children = metaNode match {
			case _: MetaNodePhysical[Attribute, Relation] => metaNode.children
			case minorNode: MetaNodeMinor[Attribute, Relation] => minorNode.children ++ minorNode.origin
		}
		metaNode match {
			case physicalNode: MetaNodePhysical[Attribute, Relation] if children.isEmpty =>
				return new ScanNode(physicalNode.originalHyperEdge) { joinTree = TreeNode(metaNode, Set.empty) } // Leaf node
			case _ => ()
		}
		for (child <- children) {
			if (!metaNode.planFromNeighbour.contains(child)) {
				metaNode.planFromNeighbour(child) = planBottomUp(child)
			}
		}
		getHeuristicOptPlanToJoinNeighbours(metaNode, children.map(c => c -> metaNode.planFromNeighbour(c)).toMap, true)
	}

	def planTopDown(metaNode: MetaNode[Attribute, Relation], planAbove: PlanNode): Unit = {
		val children = metaNode match {
			case _: MetaNodePhysical[Attribute, Relation] => metaNode.children
			case minorNode: MetaNodeMinor[Attribute, Relation] => minorNode.children ++ minorNode.origin
		}
		metaNode.parent match {
			case None => ()
			case Some(parent) =>
				if (metaNode.planFromNeighbour.contains(parent)) throw new Exception(f"On ${metaNode.nodes.mkString(",")}, Parent already computed (${parent.nodes.mkString(", ")})")
				else metaNode.planFromNeighbour(parent) = planAbove
		}
		for (child <- children) {
			planTopDown(child, getHeuristicOptPlanToJoinNeighbours(metaNode, (children - child ++ metaNode.parent).map(n => n -> metaNode.planFromNeighbour(n)).toMap))
		}
	}

	/**
	 * This method uses a heuristic to find a left-deep plan to join the current node with all neighbouring nodes.
	 * The idea is to iteratively get the next neighbour such that the cost of the next join is minimized.
	 * For virtual nodes, we merge the neighbour with the smallest cardinality to the virtual node
	 * and proceed as if it is a physical node with modified set of neighbours.
	 *
	 * @param neighbours The plans to join relations in the subtree T_{this->neighbour}.
	 * @return The plan
	 */
	@tailrec
	private def getHeuristicOptPlanToJoinNeighbours(metaNode: MetaNode[Attribute, Relation], neighbours: Map[MetaNode[Attribute, Relation], PlanNode], isBottomUp: Boolean = false): PlanNode = {
		metaNode match {
			case _: MetaNodeMinor[Attribute, Relation] =>
				val (minNeighbour, planFromMinNeighbour) = neighbours.minBy((neighbour, plan) => plan.cardinality)
				val minNeighboursNeighbours = (minNeighbour match {
					case _: MetaNodePhysical[Attribute, Relation] => minNeighbour.children
					case minorNode: MetaNodeMinor[Attribute, Relation] => minorNode.children ++ minorNode.origin
				}) ++ (if isBottomUp then None else minNeighbour.parent)
				val minNeighboursNeighboursAfterPromotion = ((minNeighboursNeighbours - metaNode) ++ neighbours.map((n, _) => n)) - minNeighbour
				getHeuristicOptPlanToJoinNeighbours(minNeighbour, minNeighboursNeighboursAfterPromotion.map(n => n -> minNeighbour.planFromNeighbour.getOrElse(n, neighbours(n))).toMap, isBottomUp)

			case physicalNode: MetaNodePhysical[Attribute, Relation] =>
				val neighboursPlans = mutable.Set.from(neighbours.values)
				var plan: PlanNode = ScanNode(physicalNode.originalHyperEdge)
				while (neighboursPlans.nonEmpty) {
					val next = neighboursPlans
						.map(neighbourPlan => (
							neighbourPlan,
							if sqlIR.cardinalities != null then sqlIR.cardinalities(plan.allJoinedRelations ++ neighbourPlan.allJoinedRelations) else 1,
							if sqlIR.cardinalities != null then sqlIR.cardinalities(neighbourPlan.allJoinedRelations) else 1)
						)
						.toList
						.sortWith((a, b) => {
							val (planA, costA, cardinalityA) = a
							val (planB, costB, cardinalityB) = b
							if costA == costB then cardinalityA < cardinalityB else costA < costB
						})
						.head._1
					plan = JoinNode(plan, next)
					neighboursPlans -= next
				}
				plan.joinTree = TreeNode(metaNode, neighbours.map { case (n, plan) => plan.joinTree }.toSet)
				plan
		}
	}

	/**
	 * This method uses dynamic programming to find the optimal plan to join the current node with all neighbouring nodes.
	 * For physical nodes, assuming nodes in different subtrees cannot be joined together, this finds the real optimum.
	 * For virtual nodes, this may not necessarily be the case if we consider bushy plans, but now we do left-deep only.
	 *
	 * @param neighboursPlans The plans to join relations in the subtree T_{this->neighbour}.
	 * @return The plan
	 */
	def getActualOptPlanToJoinNeighbours(metaNode: MetaNode[Attribute, Relation], neighboursPlans: Map[MetaNode[Attribute, Relation], PlanNode]): PlanNode = {
		val plans: mutable.Map[Set[MetaNode[Attribute, Relation]], PlanNode] = mutable.Map()
		neighboursPlans.keys.toSet.subsets().toSeq.sortBy(_.size).foreach(subset => {
			subset.size match {
				case 0 => metaNode match {
					case physicalNode: MetaNodePhysical[Attribute, Relation] => plans(subset) = ScanNode(physicalNode.originalHyperEdge)
					case _: MetaNodeMinor[Attribute, Relation] => () // For virtual nodes, we do not join the current node
				}

				case 1 if metaNode.isInstanceOf[MetaNodeMinor[Attribute, Relation]] => plans(subset) = neighboursPlans(subset.head)

				case _ =>
					val neighbour = subset.minBy(neighbour => getCumulativeCost(plans(subset - neighbour), neighboursPlans(neighbour)))
					plans(subset) = JoinNode(plans(subset - neighbour), neighboursPlans(neighbour))
			}
		})
		plans(neighboursPlans.keySet)
	}
}
