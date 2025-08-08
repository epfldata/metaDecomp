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

		val startTime = System.nanoTime()
		val meta = metaGYO(sqlIR.hyperedges)
		val joinTrees = JoinTreeEnumerator.enumerate(meta).flatMap(JoinTreeEnumerator.allRotations)
		val endTime = System.nanoTime()

		// Calculate total time
		val totalTime = (endTime - startTime) / 1000 // microseconds
		println(totalTime)
	}
}
