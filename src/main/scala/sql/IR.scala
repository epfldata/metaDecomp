package sql

import decompositions.{FilterCondition, OutputItem}
import decompositions.Hypergraph.Hyperedge

case class IR(hyperedges: Set[Hyperedge],
							filterConditions: Map[Set[Hyperedge], Set[FilterCondition]],
							// set of relations -> Set of filter conditions
							vertexIdToColumnName: Map[String, Map[Hyperedge, String]],
							// hypergraph vertex ID -> (hyperedge -> column name)
							outputAttributes: Set[OutputItem]) {
	var cardinalities: Map[Set[Hyperedge], Double] = Map.empty
}
