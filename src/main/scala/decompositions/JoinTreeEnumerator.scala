package decompositions

import decompositions.Hypergraph.{Vertex, Hyperedge}

import scala.collection.mutable

object JoinTreeEnumerator {
	def enumerate(node: MetaNode): mutable.ListBuffer[TreeNode] = {
		val sortedChildren = node.children.toList.sortWith((a, b) => b.keys.subsetOf(a.keys))

		val possibleBaseTrees: Iterable[TreeNode] = node match {
			case _: MetaNodePhysical => List(TreeNode(node, Set()))

			case minorNode: MetaNodeMinor =>
				val includeParent = minorNode.keys == minorNode.nodes && minorNode.parent.nonEmpty
				// In this case, we include the parent in the enumerated trees.
				// At the end, we reroot to that parent and the virtual node may become multiple trees under the parent
				val childrenPossibleTrees = minorNode.origin.map(enumerate(_).map(tree => if tree.metaNode == node then tree.children else Set(tree))).toList
				// Possible trees for each child
				val childrenTreeMappings = allMappings(childrenPossibleTrees)
					.map(_.flatten.toIndexedSeq // Each combination is now simply a list of trees
						++ (if includeParent then Some(TreeNode(minorNode.parent.get, Set())) else None)
					)
				// Mappings from each child to one possible tree
				for (subtrees <- childrenTreeMappings;
						 sequence <- pruferSequences(if includeParent then childrenPossibleTrees.size + 1 else childrenPossibleTrees.size))
				yield {
					val tree = spanningTreeFromPruferSequence(sequence, subtrees)
					if includeParent then tree.rerootTo(minorNode.parent.get).get else tree
				}
		}

		val childrenPossibleSubtrees: List[mutable.ListBuffer[TreeNode]] = sortedChildren.map(enumerate)

		val acc = mutable.ListBuffer.empty[TreeNode]
		for (base <- possibleBaseTrees; subtreesWithKeys <- allMappings(childrenPossibleSubtrees).map(_.zip(sortedChildren.map(_.keys)))) {
			merge(base, subtreesWithKeys, acc)
		}
		acc
	}

	// Enumerate spanning trees based on Prüfer sequence: https://en.wikipedia.org/wiki/Prüfer_sequence

	private val pruferSequencesMemo = mutable.Map[Int, mutable.ListBuffer[Seq[Int]]]()
	private def pruferSequences(n: Int): mutable.ListBuffer[Seq[Int]] = pruferSequencesMemo.getOrElse(n, {
		def rec(accCurrent: Seq[Int], accTotal: mutable.ListBuffer[Seq[Int]]): mutable.ListBuffer[Seq[Int]] = {
			if (accCurrent.size == n - 2) accTotal += accCurrent
			else (0 until n).foreach(next => rec(accCurrent :+ next, accTotal))
			accTotal
		}
		rec(List(), mutable.ListBuffer.empty)
	})

	def spanningTreeFromPruferSequence(sequence: Seq[Int], nodes: IndexedSeq[TreeNode]): TreeNode = {
		val nodesCopy = nodes.map(_.shallowCopy)
		val degree = mutable.IndexedSeq.fill(nodesCopy.size)(1)
		val sequenceSet = sequence.toSet
		val degreeOneNodes = mutable.Queue.empty[Int]
		nodesCopy.indices.foreach(i => if (!sequenceSet.contains(i)) degreeOneNodes.enqueue(i))
		sequence.foreach(degree(_) += 1)
		sequence.foreach(parentIndex => {
			val childIndex = degreeOneNodes.head
			degree(childIndex) -= 1
			degreeOneNodes.dequeue()
			degree(parentIndex) -= 1
			if (degree(parentIndex) == 1) {
				degreeOneNodes.enqueue(parentIndex)
			}
			nodesCopy(parentIndex).children += nodesCopy(childIndex)
		})
		val rootIndex = degreeOneNodes.head
		degreeOneNodes.dequeue()
		nodesCopy(rootIndex).children += nodesCopy(degreeOneNodes.head)
		nodesCopy(rootIndex)
	}

	private def collectAllRotations(root: TreeNode, parent: TreeNode, keys: Set[Vertex], acc: mutable.ListBuffer[TreeNode]): mutable.ListBuffer[TreeNode] = {
		acc += root
		root.children.foreach(c =>
			if (keys.subsetOf(c.nodes) && c != parent) {
				val currentRootCopy = TreeNode(root.metaNode,
					root.children - c
				)
				val newRoot = TreeNode(c.metaNode,
					c.children + currentRootCopy)
				collectAllRotations(newRoot, currentRootCopy, keys, acc)
			}
		)
		acc
	}

	def collectAllRotations(acc: mutable.ListBuffer[TreeNode], root: TreeNode): mutable.ListBuffer[TreeNode] = {
		collectAllRotations(root, TreeNode(MetaNodeMinor(Set(), Set()), Set()), Set(), acc)
	}

	def allRotations(root: TreeNode, keys: Set[Vertex]): mutable.ListBuffer[TreeNode] = {
		collectAllRotations(root, TreeNode(MetaNodeMinor(Set(), Set()), Set()), keys, mutable.ListBuffer.empty)
	}

	// All combinations of choices of subtree for each child
	private def allMappings[T](codomains: List[Iterable[T]]): mutable.ListBuffer[List[T]] = {
		collectAllMappings(codomains, List(), mutable.ListBuffer.empty)
	}

	private def collectAllMappings[T](codomains: List[Iterable[T]], accCurrent: List[T], acc: mutable.ListBuffer[List[T]]): mutable.ListBuffer[List[T]] = {
		codomains match {
			case Nil => acc += accCurrent
			case head :: tail => head.foreach(headOption => collectAllMappings(tail, accCurrent.appended(headOption), acc))
		}
		acc
	}

	private def merge(base: TreeNode, rest: List[(TreeNode, Set[Vertex])], acc: mutable.ListBuffer[TreeNode]): mutable.ListBuffer[TreeNode] = rest match {
		case Nil => acc += base
		case (nextTree, nextKeys) :: rest =>
			if nextTree.metaNode.isInstanceOf[MetaNodeMinor] && nextTree.metaNode.keys == nextTree.metaNode.nodes then
				merge(base, nextTree.children.toList.map(c => (c, nextKeys)) ++ rest, acc)
			else
				for (targetNode <- base.descendentsContaining(nextKeys) ;
						 rerooting <- if nextTree.metaNode == base.metaNode then Set(nextTree)
						 							else allRotations(nextTree, nextKeys)) {
					merge(base.attachingNewSubtree(targetNode, rerooting, nextTree.metaNode == base.metaNode), rest, acc)
				}
		acc
	}
}
