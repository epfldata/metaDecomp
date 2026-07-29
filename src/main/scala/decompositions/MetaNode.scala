package decompositions

import decompositions.Hypergraph.{Hyperedge, Vertex}

import scala.collection.mutable

trait MetaNode(val nodes : Set[Vertex]) {
  val id: String

  var keys : Set[Vertex] = Set()
  var children: Set[MetaNode] = Set()
  var parent: Option[MetaNode] = None
  var order: Int = 0

  def childrenPlusOrigin: Set[MetaNode]

  def toString(depth: Int): String

  def collectDescendents: Set[MetaNode] = childrenPlusOrigin.flatMap(_.collectDescendents) + this

  def computeTopoOrder(): Unit = {
    var counter = 0
    val queue: mutable.Queue[MetaNode] = mutable.Queue(this)
    while (queue.nonEmpty) {
      val current = queue.dequeue()
      current.order = counter
      counter += 1
      current.childrenPlusOrigin.foreach(queue.enqueue)
    }
  }

  var planFromNeighbour : mutable.Map[MetaNode, PlanNode] = mutable.Map() // Optimal plan for neighbour -> this
  var planWithCurrentAsRoot: PlanNode = null // Optimal plan assuming this is the root

  def toDot(implicit sqlIR: sql.IR): String
  def toDotNonRoot(parentName: String)(implicit sqlIR: sql.IR): String
}

class MetaNodePhysical(override val nodes : Set[Vertex], val originalHyperEdge: Hyperedge) extends MetaNode(nodes) {
  override val id: String = originalHyperEdge.toString

  override def childrenPlusOrigin: Set[MetaNode] = children

  def toString(depth: Int): String = {
    val outerIndent = "| " * (2 * depth)
    val innerIndent = "| " * (2 * depth + 1)
    s"""
${outerIndent}MetaNodePhysical ${id} {
${innerIndent}nodes: ${nodes.mkString(", ")}
${innerIndent}keys: ${keys.mkString(", ")}
${innerIndent}parent nodes: ${if (parent.isEmpty) "null" else parent.get.nodes.mkString(", ")}
${innerIndent}children:${children.map(_.toString(depth + 1)).mkString(",")}
$outerIndent}"""
  }

  override def toString: String = toString(0)

  def toDot(implicit sqlIR: sql.IR): String = {
    val name = originalHyperEdge.alias
    s"""graph\"\" {
  $name ;
	$name [label = \"$name\"] ;
"""
      + children.map(_.toDotNonRoot(name)).mkString("")
      + "}"
  }

  def toDotNonRoot(parentName: String)(implicit sqlIR: sql.IR): String = {
    val name = originalHyperEdge.alias
    s"""  $parentName -- $name ;
  $name [label = \"$name\"] ;
"""
      + children.map(_.toDotNonRoot(name)).mkString("")
  }
}

class MetaNodeMinor(override val nodes: Set[Vertex],var origin : Set[MetaNode]) extends MetaNode(nodes) {
  override val id: String = nodes.mkString("minor_", "_", "")

  override def childrenPlusOrigin: Set[MetaNode] = children ++ origin

  def toString(depth: Int): String = {
    val outerIndent = "| " * (2 * depth)
    val innerIndent = "| " * (2 * depth + 1)
    s"""
${outerIndent}MetaNodeMinor ${id} {
${innerIndent}nodes: ${nodes.mkString(", ")}
${innerIndent}keys: ${keys.mkString(", ")}
${innerIndent}parent nodes: ${if (parent.isEmpty) "null" else parent.get.nodes.mkString(", ")}
${innerIndent}origin:${origin.map(_.toString(depth + 1)).mkString(",")}
${innerIndent}children:${children.map(_.toString(depth + 1)).mkString(",")}
$outerIndent}"""
  }

  override def toString: String = toString(0)

  def rotation(tree: TreeNode, parent: TreeNode, keys: Set[Vertex]): List[TreeNode] = List()

  def toDot(implicit sqlIR: sql.IR): String = {
    val name = nodes.map(_.name).mkString("minor_", "_", "")
    s"""graph \"\" {
	$name ;
	$name [label = \"$name\"] ;
"""
      + childrenPlusOrigin.map(_.toDotNonRoot(name)).mkString("")
      + "}"
  }

  def toDotNonRoot(parentName: String)(implicit sqlIR: sql.IR): String = {
    val name = nodes.map(_.name).mkString("minor_", "_", "")
    s"""  $parentName -- $name ;
	$name [label = \"$name\"] ;
"""
      + childrenPlusOrigin.map(_.toDotNonRoot(name)).mkString("")
  }
}

class TreeNode(val metaNode: MetaNode, var children: Set[TreeNode]) {
  def nodes: Set[Vertex] = metaNode.nodes

  def shallowCopy: TreeNode = TreeNode(metaNode, children)

  // Copy the tree and attach a subtree to a specified node, reusing the existing tree structure whenever possible.
  // dropRoot means we are attaching a virtual node whose enumerated trees will be rooted at its parent.
  // In this case we ignore the root but add the children.
  def attachingNewSubtree(targetToAttach: TreeNode, newChild: TreeNode, dropRoot: Boolean): TreeNode =
    if (targetToAttach == this) {
      TreeNode(metaNode, children ++ (if dropRoot then newChild.children else Set(newChild)))
    } else {
      val newChildren = children.map(c => (c, c.attachingNewSubtree(targetToAttach, newChild, dropRoot)))
      if (newChildren.exists { (oldChild, newChild) => oldChild != newChild }) {
        // If the new subtree is attached to any descendent of the current node, we have to make a copy.
        TreeNode(metaNode, newChildren.map { (oldChild, newChild) => newChild })
      } else { // Otherwise, simply reuse the current node.
        this
      }
    }

  def descendentsContaining(keys: Set[Vertex]): Set[TreeNode] = {
    if (!keys.subsetOf(this.nodes)) Set()
    else children.flatMap(_.descendentsContaining(keys)) + this
  }

  def rerootTo(metaNode: MetaNode): Option[TreeNode] = {
    if (this.metaNode == metaNode) Some(this)
    else children.flatMap(c => c.rerootTo(metaNode) match {
      case Some(newRoot) =>
        c.children += this
        this.children -= c
        Some(newRoot)
      case None => None
    }).headOption
  }

  override def toString: String = toString(0)

  def toString(depth: Int): String = {
    val outerIndent = "| " * (2 * depth)
    val innerIndent = outerIndent + "| "
    s"""
${outerIndent}TreeNode {
${innerIndent}nodes: ${nodes.mkString(", ")}
${innerIndent}children:${children.map(_.toString(depth + 1)).mkString("", ",", "\n")}$outerIndent}"""
  }

  def toJson(implicit sqlIR: sql.IR): String = toJson(0)

  private def toJson(depth: Int)(implicit sqlIR: sql.IR): String = {
    val outerIndent = "  " * (depth * 2)
    val innerIndent = "  " * (depth * 2 + 1)
    s"""$outerIndent{
$innerIndent\"relation\": \"${this.metaNode.asInstanceOf[MetaNodePhysical].originalHyperEdge.alias}\",
$innerIndent\"children\": [
${children.map(_.toJson(depth + 1)).mkString(",\n")}
$innerIndent]
$outerIndent}"""
  }
}
