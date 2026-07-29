package decompositions

import scala.annotation.tailrec
import scala.collection.mutable

import Hypergraph.{Vertex, Hyperedge, Component, HyperedgeSetExtension}

trait NamedElement(val name: String)
implicit class NamedElementSetExtension[T <: NamedElement](val s: Set[T]) {
	def asString: String = {
		s.map(_.name).toList.sorted.mkString(",")
	}
}

object Hypergraph {
	case class Vertex(override val name: String) extends NamedElement(name) {
		override def toString: String = name
	}

	case class Hyperedge(val nodes: Set[Vertex], tableName: String = "", alias: String = "") extends NamedElement(alias) {
		def contains(v: Vertex)      : Boolean = nodes.contains(v)
		def overlaps(S: Set[Vertex]) : Boolean = (! nodes.intersect(S).isEmpty)
	}

	implicit class HyperedgeSetExtension(val s: Iterable[Hyperedge]) {
		private val hyperedgeSetVertices = mutable.Map.empty[Set[Hyperedge], Set[Vertex]]
		def nodes: Set[Vertex] = {
			val set = s.toSet
			hyperedgeSetVertices.getOrElseUpdate(set,
			set.flatMap(_.nodes)
			)
		}
	}

	type Component = Set[Vertex]
	type Separator = Set[Hyperedge]
}

case class Hypergraph(var vertices: Set[Hypergraph.Vertex], var edges: Set[Hypergraph.Hyperedge]) {

	val adjVertices: Map[Hypergraph.Vertex, Set[Hypergraph.Vertex]] = vertices.map(v => v -> edges.filter(_.nodes.contains(v)).nodes).toMap

	override def equals(obj: Any): Boolean = obj match {
		case Hypergraph(otherVertices, otherEdges) =>
			this.vertices == otherVertices && this.edges == otherEdges
		case _ => false
	}

	val edgesInComponentMemp = mutable.Map.empty[Component, Set[Hyperedge]]

	def edgesInComponent(component: Component): Set[Hyperedge] = {
		edgesInComponentMemp.getOrElseUpdate(component, this.edges.filter(_.nodes.exists(component.contains)))
	}

	override def toString: String = {
		edges.map { e =>
			val nodesStr = e.nodes.map {
				case n: NamedElement => n.name
				case other => other.toString
			}.toList.sorted.mkString(",")
			s"${e.toString}($nodesStr)"
		}.mkString("\n")
	}

	private val componentsMemo = mutable.Map.empty[(Set[Vertex], Set[Vertex]), Set[Component]]

	def componentsInducedBy(vertices: Set[Vertex], allowedVertices: Set[Vertex] = this.vertices): Set[Component] = {
		def computeComponents(vertices: Set[Vertex]): Set[Component] = {
			val components = mutable.Set.empty[Component]
			val unvisited = mutable.Set.from(allowedVertices -- vertices)
			while (unvisited.nonEmpty) {
				@tailrec
				def walk(visited: Set[Vertex]): Set[Vertex] = {
					val updated = visited ++ visited.flatMap(adjVertices).intersect(allowedVertices) -- vertices
					if updated == visited then updated else walk(updated)
				}

				val newComponent = walk(Set(unvisited.head))
				components += newComponent
				unvisited --= newComponent
			}
			Set.from(components)
		}

		componentsMemo.getOrElseUpdate((vertices, allowedVertices), computeComponents(vertices)) // calcuating hash is slow
	}
	
	def isConnected(edges: Set[Hyperedge]): Boolean = {
		if (edges.isEmpty) return true
		val e1 = edges.head
		val reachableVertices = mutable.Set.empty[Vertex]
		val remainingEdges = mutable.Set.from(edges)
		val adjEdges = mutable.Map.empty[Vertex, mutable.Set[Hyperedge]]
		edges.foreach(e => e.nodes.foreach(v => adjEdges.getOrElseUpdate(v, mutable.Set.empty) += e))
		val queue = mutable.Queue.from(List(e1))
		while (queue.nonEmpty) {
			val e = queue.dequeue()
			remainingEdges -= e
			for (v <- e.nodes.diff(reachableVertices)) {
				for (otherEdge <- adjEdges.getOrElse(v, mutable.Set.empty).intersect(remainingEdges)) {
					queue += otherEdge
				}
			}
			reachableVertices ++= e.nodes
		}
		remainingEdges.isEmpty

		// if (edges.isEmpty) return true
		// val e1 = edges.head
		// val reachableVertices = mutable.Set.from(e1.nodes)
		// val remainingEdges = mutable.Set.from(edges - e1)
		// while (remainingEdges.nonEmpty && remainingEdges.flatMap(_.nodes).intersect(reachableVertices).nonEmpty) {
		// 	remainingEdges.foreach(e => if (e.nodes.intersect(reachableVertices).nonEmpty) {
		// 		reachableVertices ++= e.nodes
		// 		remainingEdges -= e
		// 	})
		// }
		// reachableVertices.size == edges.flatMap(_.nodes).size
	}
}

