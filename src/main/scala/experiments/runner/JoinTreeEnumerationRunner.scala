package experiments.runner

import decompositions.*
import sql.SQLParser
import experiments.readInOneLine

import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.collection.mutable
import scala.io.Source

object JoinTreeEnumerationRunner {
	def main(args: Array[String]): Unit = {
		if (args.length != 1) {
			println("Usage: JoinTreeEnumerationRunner <sql_file>")
			sys.exit(1)
		}

		val sqlFile = args(0)

		val query = readInOneLine(new java.io.File(sqlFile))

		// Parse the query
		val sqlIR = SQLParser.parse(query)

		print(s"${sqlIR.hyperedges.size},")

		try {
			val startTime = System.nanoTime()
			val meta = metaGYO(sqlIR.hyperedges).get
			val joinTrees = mutable.ListBuffer.empty[decompositions.TreeNode[sql.Attribute, sql.Relation]]
			JoinTreeEnumerator.enumerate(meta).foreach(tree => JoinTreeEnumerator.collectAllRotations(joinTrees, tree))
			val endTime = System.nanoTime()

			// Calculate total time
			val totalTime = (endTime - startTime) / 1000 // microseconds
			println(s"${joinTrees.size},${totalTime}")
		} catch {
			case e: Throwable =>
				println(e.getMessage)
				sys.exit(1)
		}
	}
}
