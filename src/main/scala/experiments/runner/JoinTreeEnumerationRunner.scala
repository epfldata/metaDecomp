package experiments.runner

import decompositions.*
import sql.SQLParser

import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.collection.mutable
import scala.io.Source

object JoinTreeEnumerationRunner {
	def main(args: Array[String]): Unit = {
		if (args.length != 2) {
			println("Usage: JoinTreeEnumerationRunner <input_file> <output_file>")
			sys.exit(1)
		}

		val inputFile = args(0)
		val outputFile = args(1)

		// Read the SQL query from the input file
		val source = Source.fromFile(inputFile)
		val query = source.getLines().mkString(" ")
		source.close()

		// Parse the query
		val sqlIR = SQLParser.parse(query)

		Files.write(Paths.get(outputFile), s"${Paths.get(inputFile).getFileName},${sqlIR.hyperedges.size},".getBytes, StandardOpenOption.APPEND)

		// Run MetaGYO
		val metaGYOStartTime = System.nanoTime()
		val meta = metaGYO(sqlIR.hyperedges)
		val metaGYOEndTime = System.nanoTime()
		val metaGYOTime = (metaGYOEndTime - metaGYOStartTime) / 1000 // microseconds

		// Run enumeration
		val enumStartTime = System.nanoTime()
		val joinTrees = JoinTreeEnumerator.enumerate(meta).flatMap(JoinTreeEnumerator.allRotations)
		val enumEndTime = System.nanoTime()
		val enumTime = (enumEndTime - enumStartTime) / 1000 // microseconds

		// Calculate total time
		val totalTime = metaGYOTime + enumTime

		// Write results to the output file
		val resultsCsvLine = s"$metaGYOTime,$enumTime,$totalTime"
		Files.write(Paths.get(outputFile), resultsCsvLine.getBytes, StandardOpenOption.APPEND)

		println(s"Enumeration completed")
	}
}
