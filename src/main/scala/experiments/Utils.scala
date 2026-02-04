package experiments

import experiments.Config.benchmarksPath

import java.io.File
import scala.io.Source
import scala.collection.BitSet

def readInOneLine(file: File): String = {
	val source = Source.fromFile(file)
	val result = source.getLines().mkString(" ")
	source.close()
	result
}

def parseSubqueryTables(lines: Iterator[String]): Iterator[Set[String]] = {
	lines.next()
	val tables = lines.next().split(" ").map(_.trim).toList
	lines.next()
	lines.map(_.split(" ").map(_.trim).head).map(_.toLong).map(bitMask => tables.indices.filter(i => ((bitMask >> i) & 1) == 1).map(tables).toSet)
}

def getTimestamp: String = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace(":", "-").replaceAll("""\..*""", "")

extension (s: Iterable[Double]) {
	def median(): Double = {
		if (s.size % 2 == 0) {
			(s.toList.sorted.apply(s.size / 2) + s.toList.sorted.apply(s.size / 2 - 1)) / 2
		} else {
			s.toList.sorted.apply(s.size / 2)
		}
	}
}
