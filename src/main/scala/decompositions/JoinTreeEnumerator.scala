package decompositions

import scala.collection.mutable

object JoinTreeEnumerator {
	def enumerate[VT, ET <: HyperEdge[VT]](node: MetaNode[VT, ET]): Set[TreeNode[VT, ET]] = {
		val sortedChildren = node.children.toList.sortWith((a, b) => b.keys.subsetOf(a.keys))

		val possibleBaseTrees: Iterable[TreeNode[VT, ET]] = node match {
			case _: MetaNodePhysical[VT, ET] => List(TreeNode[VT, ET](node, Set()))

			case minorNode: MetaNodeMinor[VT, ET] =>
				val includeParent = minorNode.keys == minorNode.nodes && minorNode.parent.nonEmpty
				// In this case, we include the parent in the enumerated trees.
				// At the end, we reroot to that parent and the virtual node may become multiple trees under the parent
				val childrenPossibleTrees = minorNode.origin.map(enumerate(_).map(tree => if tree.metaNode == node then tree.children else Set(tree))).toList
				// Possible trees for each child
				val childrenTreeMappings = allMappings(childrenPossibleTrees)
					.map(_.flatten.toIndexedSeq // Each combination is now simply a list of trees
						++ (if includeParent then Some(TreeNode[VT, ET](minorNode.parent.get, Set())) else None)
					)
				// Mappings from each child to one possible tree
				for (subtrees <- childrenTreeMappings;
						 sequence <- pruferSequences(if includeParent then childrenPossibleTrees.size + 1 else childrenPossibleTrees.size))
				yield {
					val tree = spanningTreeFromPruferSequence(sequence, subtrees)
					if includeParent then tree.rerootTo(minorNode.parent.get).get else tree
				}
		}

		val childrenPossibleSubtrees: List[Set[TreeNode[VT, ET]]] = sortedChildren.map(enumerate)

		(for base <- possibleBaseTrees; subtreesWithKeys <- allMappings(childrenPossibleSubtrees).map(_.zip(sortedChildren.map(_.keys)))
			yield merge(base, subtreesWithKeys)
			).flatten.toSet
	}

	// Enumerate spanning trees based on Prüfer sequence: https://en.wikipedia.org/wiki/Prüfer_sequence

	private def pruferSequences(n: Int): List[Seq[Int]] = {
		def rec(acc: Seq[Int]): List[Seq[Int]] = {
			if (acc.size == n - 2) List(acc)
			else (0 until n).flatMap(next => rec(acc :+ next)).toList
		}
		rec(List())
	}

	def spanningTreeFromPruferSequence[VT, ET <: HyperEdge[VT]](sequence: Seq[Int], nodes: IndexedSeq[TreeNode[VT, ET]]): TreeNode[VT, ET] = {
		val nodesCopy = nodes.map(_.shallowCopy)
		val degree = mutable.IndexedSeq.fill(nodesCopy.size)(1)
		val degreeOneNodes = mutable.TreeSet.from(nodesCopy.indices)(Ordering[Int].reverse) -- sequence
		sequence.foreach(degree(_) += 1)
		sequence.foreach(parentIndex => {
			val childIndex = degreeOneNodes.head
			degree(childIndex) -= 1
			degreeOneNodes -= childIndex
			degree(parentIndex) -= 1
			if (degree(parentIndex) == 1) {
				degreeOneNodes += parentIndex
			}
			nodesCopy(parentIndex).children += nodesCopy(childIndex)
		})
		val rootIndex = degreeOneNodes.head
		degreeOneNodes -= rootIndex
		nodesCopy(rootIndex).children += nodesCopy(degreeOneNodes.head)
		nodesCopy(rootIndex)
	}

	private def allRotations[VT, ET <: HyperEdge[VT]](root: TreeNode[VT, ET], parent: TreeNode[VT, ET], keys: Set[VT]): Set[TreeNode[VT, ET]] = {
		root.children.flatMap(c =>
			if (keys.subsetOf(c.nodes) && c != parent) {
				val currentRootCopy = TreeNode[VT, ET](root.metaNode, root.children - c)
				val newRoot = TreeNode[VT, ET](c.metaNode, c.children + currentRootCopy)
				allRotations(newRoot, currentRootCopy, keys) + newRoot
			} else {
				Set.empty
			}
		) + root
	}

	private def allRotations[VT, ET <: HyperEdge[VT]](root: TreeNode[VT, ET], keys: Set[VT]): Set[TreeNode[VT, ET]] =
		allRotations(root, TreeNode[VT, ET](MetaNodeMinor[VT, ET](Set(), Set()), Set()), keys)

	def allRotations[VT, ET <: HyperEdge[VT]](root: TreeNode[VT, ET]): Set[TreeNode[VT, ET]] = {
		allRotations(root, TreeNode[VT, ET](MetaNodeMinor[VT, ET](Set(), Set()), Set()), Set())
	}

	// All combinations of choices of subtree for each child
	private def allMappings[T](codomains: List[Iterable[T]]): List[List[T]] = codomains match {
		case Nil => List(Nil)
		case head :: tail => head.toList.flatMap(headOption =>
			allMappings(tail).map(tailMapping => headOption :: tailMapping)
		)
	}

	private def merge[VT, ET <: HyperEdge[VT]](base: TreeNode[VT, ET], rest: List[(TreeNode[VT, ET], Set[VT])]): Set[TreeNode[VT, ET]] = rest match {
		case Nil => Set(base)
		case (nextTree, nextKeys) :: rest =>
			if nextTree.metaNode.isInstanceOf[MetaNodeMinor[VT, ET]] && nextTree.metaNode.keys == nextTree.metaNode.nodes then
				merge(base, nextTree.children.toList.map(c => (c, nextKeys)) ++ rest)
			else
				(for targetNode <- base.descendentsContaining(nextKeys)
						 rerooting <- if nextTree.metaNode == base.metaNode then Set(nextTree)
						 else allRotations(nextTree, nextKeys)
				yield merge(base.attachingNewSubtree(targetNode, rerooting, nextTree.metaNode == base.metaNode), rest)
					).flatten
	}
}
