package decompositions



trait HyperEdge[V] {
  val nodes: Set[V]

  def contains(v: V)      : Boolean = nodes.contains(v)
  def overlaps(S: Set[V]) : Boolean = (! nodes.intersect(S).isEmpty)
}



extension [V](E: Iterable[HyperEdge[V]])
  def nodes = E.flatMap(_.nodes).toSet

