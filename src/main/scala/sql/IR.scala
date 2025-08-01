package sql

import decompositions.{FilterCondition, HyperEdge, OutputItem}

case class IR(hyperedges: Set[Relation],
							filterConditions: Map[Set[Relation], Set[FilterCondition]],
							// set of relations -> Set of filter conditions
							vertexIdToColumnName: Map[String, Map[Relation, String]],
							// hypergraph vertex ID -> (hyperedge -> column name)
							outputAttributes: Set[OutputItem]) {
	var cardinalities: Map[Set[Relation], Long] = Map.empty
}
