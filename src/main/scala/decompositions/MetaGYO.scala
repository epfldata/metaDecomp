package decompositions

import scala.collection.mutable
import scala.util.boundary, boundary.break

def findMinor[VT, ET <: HyperEdge[VT]](set: collection.mutable.Set[MetaNode[VT, ET]], node: Set[VT]): Option[MetaNodeMinor[VT, ET]] = {
  set.collectFirst { case minor: MetaNodeMinor[VT, ET] if minor.nodes == node => minor }
}

def constructEdges[VT, ET <: HyperEdge[VT]](set: collection.mutable.Set[MetaNode[VT, ET]]): Unit = {
  for (node <- set) {
    boundary {
      val minor = findMinor(set, node.keys)
      if (minor.isDefined && minor.get != node) {
        if (!minor.get.origin.contains(node)) {
          minor.get.origin = minor.get.origin + node
          //throw new Exception("Minor node is not in origin, check!") It may be possible that the minor node for the new design
        }
        node.parent = Some(minor.get)
      } else {
        for (n <- set) {
          if (n != node && node.keys.subsetOf(n.nodes) && !node.keys.subsetOf(n.keys)) {
            if (node.parent.nonEmpty) throw new Exception("Parent already set")
            if (node.children.contains(n)) throw new Exception("Children already exists!")
            node.parent = Some(n)
            n.children = n.children + node
            break()
          }
        }
      }
    }
  }
}

def metaGYO[VT, ET <: HyperEdge[VT]](E: Iterable[ET]) : MetaNode[VT, ET] = {

  val V = E.nodes
  val metaGYOGraph = collection.mutable.Set[MetaNode[VT, ET]]()
  // hyperedges not eaten away as ears yet, with previously removed ears
  // attached as tree nodes
  // Line 3-4
  val E2 = collection.mutable.Set[MetaNode[VT, ET]]() ++ E.map(x => MetaNodePhysical[VT, ET](x.nodes, x))

  // how many hyperedges does a node occur in?
  val Vcnt = collection.mutable.Map[VT, Int](V.toSeq.map((_, 0)): _*)
  E.foreach(_.nodes.foreach(Vcnt(_) += 1))


  while (E2.nonEmpty) {
    // First generate all the minor nodes
    val E3 = collection.mutable.Set[MetaNode[VT, ET]]() ++ E2
    val newMinor = collection.mutable.Set[MetaNode[VT, ET]]()

    // Line 6-14
    while E3.nonEmpty do {
      val e = E3.head
      var isMinor = false
      var minor_e: MetaNodeMinor[VT, ET] = null
      for e3 <- E3.iterator do {
        if (e != e3) {
          val shared_e = e.nodes.filter(Vcnt(_) > 1)
          val shared_e3 = e3.nodes.filter(Vcnt(_) > 1)
          // If a star is detected, remove e3 from the list of hyperedges, and update the Vcnt.
          if (shared_e.equals(shared_e3)) {
            if (!isMinor) {
              e.keys = shared_e
              e3.keys = shared_e3
              // Need to check if the virtual node already exists
              val findResult = findMinor(metaGYOGraph, shared_e)
              minor_e = if (findResult.isDefined) {
                // If exists a virtual node from previous rounds with \chi equal to \kappa(e), then merge them
                // Line 10-11
                metaGYOGraph.remove(findResult.get)
                findResult.get.keys = Set()
                findResult.get.origin = findResult.get.origin + e + e3
                findResult.get
              } else {
                // Line 8
                MetaNodeMinor[VT, ET](shared_e, Set(e, e3))
              }
              isMinor = true
            } else {
              minor_e.origin = minor_e.origin + e3
              e3.keys = shared_e3
            }
            // Line 12
            E2.remove(e3)
            E3.remove(e3)
            metaGYOGraph.add(e3)
            shared_e.foreach(Vcnt(_) -= 1)
          }
        }
      }
      // If a minor node was found, add it to the list of minor nodes
      if (isMinor) {
        newMinor.add(minor_e)
        metaGYOGraph.add(e)
        E2.remove(e)
      }
      E3.remove(e)
    }

    // Now add the new minor nodes to the list of hyperedges
    E2.addAll(newMinor)


    // a temporary set to store the ears
    val tempEars = collection.mutable.Set[MetaNode[VT, ET]]()
    val E4 = collection.mutable.Set[MetaNode[VT, ET]]() ++ E2
    // calculate all ears for this round, Line 12
    while E4.nonEmpty do {
      val e = E4.head
      E4.remove(e)
      val shared_e = e.nodes.filter(Vcnt(_) > 1)
      boundary {
        for e4 <- E2.iterator do {
          if (e4 != e && shared_e.subsetOf(e4.nodes)) {
            tempEars.add(e)
            break()
          }
        }
      }
    }

    // remove the ears from the list of hyperedges
    for e <- tempEars do {
      val shared_e = e.nodes.filter(Vcnt(_) > 1)
      e.keys = shared_e
      // If a minor node can be constructed between the ear and an existing node, do so
      boundary {
        for e1 <- metaGYOGraph do {
          // If two nodes have the same key, try to find a minor or create a new one
          if (e1.keys.equals(e.keys) &&
            !(e.isInstanceOf[MetaNodeMinor[VT, ET]] && e.asInstanceOf[MetaNodeMinor[VT, ET]].origin.contains(e1))) {
            (e1, e) match {
              case (x : MetaNodeMinor[VT, ET], y : MetaNodeMinor[VT, ET]) if x.nodes.equals(x.keys) && y.nodes.equals(y.keys) =>
                throw new Exception("Two minor nodes with the same chi found")
              case (x : MetaNodeMinor[VT, ET], y : MetaNode[VT, ET]) if x.nodes.equals(x.keys) =>
                x.origin = x.origin + y
              case (x : MetaNode[VT, ET], y : MetaNodeMinor[VT, ET]) if y.nodes.equals(y.keys) =>
                y.origin = y.origin + x
              case _ =>
                findMinor(metaGYOGraph, shared_e) match {
                  case Some(minor) =>
                    minor.keys = shared_e
                    minor.origin ++= List(e, e1)
                  case None => metaGYOGraph.add(new MetaNodeMinor[VT, ET](shared_e, Set(e, e1)) { keys = shared_e })
                }
            }
            break()
          }
        }
      }

      shared_e.foreach(Vcnt(_) -= 1)

      E2.remove(e)
      metaGYOGraph.add(e)
    }

    if (E2.size == 1) {
      val e = E2.head
      E2.remove(e)
      metaGYOGraph.add(e)
    }
  }
  constructEdges(metaGYOGraph)
  metaGYOGraph.find(_.parent.isEmpty).get
}
