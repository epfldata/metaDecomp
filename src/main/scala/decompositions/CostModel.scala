package decompositions

import decompositions.Hypergraph.Hyperedge

object CostModel {
	def getJoinOpCost(lhs: PlanNode, rhs: PlanNode)(implicit cardinalities: Map[Set[Hyperedge], Double] = Map.empty): Double = {
		lhs.cardinality + rhs.cardinality + cardinalities(lhs.allJoinedRelations ++ rhs.allJoinedRelations)
	}

	def getCumulativeCost(lhs: PlanNode, rhs: PlanNode): Double = {
		lhs.cumulativeCost + rhs.cumulativeCost + lhs.cardinality + rhs.cardinality
	}
}
