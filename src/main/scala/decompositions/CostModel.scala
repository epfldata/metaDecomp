package decompositions

import sql.Relation

object CostModel {
	def getJoinOpCost(lhs: PlanNode, rhs: PlanNode)(implicit cardinalities: Map[Set[Relation], Long] = Map.empty): Double = {
		lhs.cardinality + rhs.cardinality + cardinalities(lhs.allJoinedRelations ++ rhs.allJoinedRelations)
	}

	def getCumulativeCost(lhs: PlanNode, rhs: PlanNode): Double = {
		lhs.cumulativeCost + rhs.cumulativeCost + lhs.cardinality + rhs.cardinality
	}
}
