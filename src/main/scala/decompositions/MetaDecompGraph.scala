package decompositions

import decompositions.Hypergraph.{Hyperedge, Vertex, Separator, Component}
import decompositions.NamedElement
import scala.collection.mutable
import decompositions.Hypergraph._

class MetaDecompGraph {
	val vertices: mutable.Set[Separator] = mutable.Set.empty
	val sortedEdges: mutable.SortedSet[(Separator, Separator, Component)] = mutable.SortedSet.empty(Ordering.by { case (e1, e2, c) => (c.size, e1.toString, e2.toString) })
	var adjList: mutable.Map[Separator, mutable.Map[Component, mutable.Set[Separator]]] = mutable.Map.empty // Separator -> (Component -> Next Separators)

	def addEdge(e1: Separator, e2: Separator, c: Component): Unit = {
		vertices += e1
		vertices += e2
		adjList.getOrElseUpdate(e1, mutable.Map.empty).getOrElseUpdate(c, mutable.Set.empty) += e2
		if (!adjList.contains(e2)) {
			adjList(e2) = mutable.Map.empty
		}
		sortedEdges += ((e1, e2, c))
	}

	def removeEdge(e1: Separator, e2: Separator, c: Component): Unit = {
		adjList(e1)(c) -= e2
		sortedEdges -= ((e1, e2, c))
	}

	def removeVertex(v: Separator): Unit = {
		if (adjList.contains(v)) {
			adjList.remove(v)
		}
		adjList.foreach { case (e1, ce2s) => ce2s.foreach { case (c, e2s) => if (e2s contains v) e2s -= v } }
		sortedEdges.filterInPlace(e => e._1 != v && e._2 != v)
	}

	override def toString: String = {
		def separatorStr(s: Separator): String = {
			s.map {
				case n: NamedElement => n.name
				case other => other.nodes.toString
			}.toList.sorted.mkString("(", ",", ")")
		}

		adjList.toSeq.sortBy(pair => separatorStr(pair._1)).map { case (e1, ce2s) =>
			val e2sStr = ce2s.flatMap(_._2).toSeq.sortBy(e => separatorStr(e)).map(e => separatorStr(e)).mkString(", ")
			s"${separatorStr(e1)} -> $e2sStr"
		}.mkString("\n")
	}

	def findComponents(s: Separator, c: Component): Seq[Component] = {
		adjList
			.get(s)
			.map(
				_.iterator
					.collect { case (comp, nextSeparators) if comp.subsetOf(c) && nextSeparators.nonEmpty => comp }
					.toSeq
			)
			.getOrElse(Seq.empty)
	}

	def countHypertreeDecompositions(): BigInt = {

		val edgeWeight = mutable.Map.empty[(Separator, Separator, Component), BigInt]


		val stateWeight = mutable.Map.empty[(Separator, Component), BigInt]

		def getStateWeight(separator: Separator,
												component: Component): BigInt = {
			stateWeight.getOrElseUpdate(
				(separator, component), {

					adjList
						.get(separator)
						.flatMap(_.get(component))
						.map { nextSeparators =>
							nextSeparators.foldLeft(BigInt(0)) { (sum, nextSeparator) =>

								val edge = (separator, nextSeparator, component)

								sum + edgeWeight.getOrElse(
									edge,
									throw new IllegalStateException(
										s"Weight of child edge $edge has not been computed yet. " +
										"This means sortedEdges is not in bottom-up component order."
									)
								)
							}
						}
						.getOrElse(BigInt(0))
				}
			)
		}

		sortedEdges.foreach { edge =>
			val (r, s, c) = edge

			val childComponents = findComponents(s, c)

			val weight =
				childComponents.foldLeft(BigInt(1)) {
					case (product, childComponent) =>
						product * getStateWeight(s, childComponent)
				}

			edgeWeight(edge) = weight
			
		}


		sortedEdges.iterator
			.filter { case (r, _, _) => r.isEmpty }
			.foldLeft(BigInt(0)) {
				case (sum, edge) =>
					sum + edgeWeight(edge)
			}
	}

	def cutLeafEdges(): Unit = {
		var toDelete: mutable.Set[(Separator, Separator, Component)] = mutable.Set.empty
		sortedEdges.foreach { edge =>
			val (r, s, c) = edge
			if (findComponents(s, c).isEmpty) {
				toDelete += edge
			}
		}

		toDelete.foreach((r, s, c) => removeEdge(r, s, c))
	}

	def cutLeavesUpToLevel(n: Int): Unit = {
		for (i <- 0 until n) {
			cutLeafEdges()
		}
	}

}
