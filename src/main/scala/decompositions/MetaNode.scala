package decompositions

import sql.RelationCopy

import scala.collection.mutable

trait MetaNode[V, E <: HyperEdge[V]](val nodes : Set[V]) {
  var keys : Set[V] = Set()
  var children: Set[MetaNode[V, E]] = Set()
  var parent: Option[MetaNode[V, E]] = None
  var order: Int = 0

  def childrenPlusOrigin: Set[MetaNode[V, E]]

  def toString(depth: Int): String

  def collectDescendents: Set[MetaNode[V, E]] = childrenPlusOrigin.flatMap(_.collectDescendents) + this

  def computeTopoOrder(): Unit = {
    var counter = 0
    val queue: mutable.Queue[MetaNode[V, E]] = mutable.Queue(this)
    while (queue.nonEmpty) {
      val current = queue.dequeue()
      current.order = counter
      counter += 1
      current.childrenPlusOrigin.foreach(queue.enqueue)
    }
  }

  var planFromNeighbour : mutable.Map[MetaNode[V, E], PlanNode] = mutable.Map() // Optimal plan for neighbour -> this
  var planWithCurrentAsRoot: PlanNode = null // Optimal plan assuming this is the root

}


class MetaNodePhysical[V, E <: HyperEdge[V]](override val nodes : Set[V], val originalHyperEdge: E) extends MetaNode[V, E](nodes) {
  override def childrenPlusOrigin: Set[MetaNode[V, E]] = children

  def toString(depth: Int): String = {
    val outerIndent = "| " * (2 * depth)
    val innerIndent = "| " * (2 * depth + 1)
    s"""
${outerIndent}MetaNodePhysical {
${innerIndent}nodes: ${nodes.mkString(", ")}
${innerIndent}keys: ${keys.mkString(", ")}
${innerIndent}parent nodes: ${if (parent.isEmpty) "null" else parent.get.nodes.mkString(", ")}
${innerIndent}children:${children.map(_.toString(depth + 1)).mkString(",")}
$outerIndent}"""
  }

  override def toString: String = toString(0)
}

class MetaNodeMinor[V, E <: HyperEdge[V]](override val nodes: Set[V],var origin : Set[MetaNode[V, E]]) extends MetaNode[V, E](nodes) {
  override def childrenPlusOrigin: Set[MetaNode[V, E]] = children ++ origin

  def toString(depth: Int): String = {
    val outerIndent = "| " * (2 * depth)
    val innerIndent = "| " * (2 * depth + 1)
    s"""
${outerIndent}MetaNodeMinor {
${innerIndent}nodes: ${nodes.mkString(", ")}
${innerIndent}keys: ${keys.mkString(", ")}
${innerIndent}parent nodes: ${if (parent.isEmpty) "null" else parent.get.nodes.mkString(", ")}
${innerIndent}origin:${origin.map(_.toString(depth + 1)).mkString(",")}
${innerIndent}children:${children.map(_.toString(depth + 1)).mkString(",")}
$outerIndent}"""
  }

  override def toString: String = toString(0)

  def rotation(tree: TreeNode[V, E], parent: TreeNode[V, E], keys: Set[V]): List[TreeNode[V, E]] = List()
}

class TreeNode[V, E <: HyperEdge[V]](val metaNode: MetaNode[V, E], var children: Set[TreeNode[V, E]]) {
  def nodes: Set[V] = metaNode.nodes

  def shallowCopy: TreeNode[V, E] = TreeNode(metaNode, children)

  // Copy the tree and attach a subtree to a specified node, reusing the existing tree structure whenever possible.
  // dropRoot means we are attaching a virtual node whose enumerated trees will be rooted at its parent.
  // In this case we ignore the root but add the children.
  def attachingNewSubtree(targetToAttach: TreeNode[V, E], newChild: TreeNode[V, E], dropRoot: Boolean): TreeNode[V, E] =
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

  def descendentsContaining(keys: Set[V]): Set[TreeNode[V, E]] = {
    if (!keys.subsetOf(this.nodes)) Set()
    else children.flatMap(_.descendentsContaining(keys)) + this
  }

  def rerootTo(metaNode: MetaNode[V, E]): Option[TreeNode[V, E]] = {
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
$innerIndent\"relation\": \"${this.metaNode.asInstanceOf[MetaNodePhysical[V, E]].originalHyperEdge.asInstanceOf[RelationCopy].alias}\",
$innerIndent\"children\": [
${children.map(_.toJson(depth + 1)).mkString(",\n")}
$innerIndent]
$outerIndent}"""
  }
}
