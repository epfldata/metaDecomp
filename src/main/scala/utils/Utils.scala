package utils

extension[T] (s: Set[T]) {
	def subsetsOfSizeAtMost(k: Int): Iterator[Set[T]] = Range.inclusive(0, k).iterator.flatMap(s.subsets(_).iterator)
}
