package sql

import decompositions.HyperEdge

case class Relation(name: String, alias: String, attributes: Set[Attribute]) extends HyperEdge[Attribute] {
	override val nodes: Set[Attribute] = attributes
	override def toString: String = "Relation(" + nodes.mkString(", ") + ")"
}
