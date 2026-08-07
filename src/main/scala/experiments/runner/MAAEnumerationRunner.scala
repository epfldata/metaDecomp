package experiments.runner

import decompositions.{Hypergraph, MetaDecompGraph, MetaDecompGraphConstructor, MAA, IncrementalMAA, PlainMAA}
import experiments.Config.*
import experiments.{readInOneLine, getTimestamp, median}
import sql.SQLParser

import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.concurrent.{Await, Future, TimeoutException}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*

object MAAEnumerationRunner {
	private val sampleSizes = List(1, 2, 10, 100, 1_000, 10_000, 100_000)

	private val enumerators: List[(String, (Hypergraph, MetaDecompGraph, Int) => Iterator[MAA.DecompositionNode])] = List(
		"lazy" -> ((h, g, _) => MAA.enumerate(h, g)),
		//"incremental" -> ((h, g, _) => IncrementalMAA.enumerate(h, g)),
		"plain" -> ((h, _, width) => PlainMAA.enumerate(h, width))
	)

	def main(args: Array[String]): Unit = {
		for (benchmark <- benchmarks) {
			val resultsPath = Paths.get(resultsDir, s"maa-enumerate-$benchmark-$getTimestamp.csv")

			var headers = "query,num_rels,width,width_search_time"
			for ((name, _) <- enumerators; n <- sampleSizes) headers += s",time_top_${n}_$name,time_all_$name"
			headers += "\n"

			Files.write(
				resultsPath,
				headers.getBytes,
				StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
			)

			sqlFilesInBenchmark(benchmark).foreach(sqlFile => {
				println("\n-----------------------------------")
				println(s"${sqlFile.getName}")

				val query = readInOneLine(sqlFile)
				val sqlIR = this.synchronized { SQLParser.parse(query) }
				val hypergraph = Hypergraph(sqlIR.hyperedges.flatMap(_.nodes).toSet, sqlIR.hyperedges)

				var width = 1
				var g = MetaDecompGraphConstructor().run(hypergraph, width)
				while (g.sortedEdges.isEmpty) {
					width += 1
					g = MetaDecompGraphConstructor().run(hypergraph, width)
				}

				val (_, widthSearchTime) = timeIt {
					var g = MetaDecompGraphConstructor().run(hypergraph, width)
				}

				println(s"Width: $width (search took $widthSearchTime us)")

				val topTimesByEnumerator = enumerators.map { case (name, enumerate) =>
					val times = sampleSizes.map(n => {
						val (decompositions, time) = timeItRepeat {
							val ds = enumerate(hypergraph, g, width).take(n).toList
							ds
						}
						println(s"[$name] Time to get top $n decomposition(s): $time us (got ${decompositions.size})")
						if (decompositions.size != n) -1 else time
					})
					/*val getAllTime = timeItRepeat {
						val ds = enumerate(hypergraph, g, width).size
						ds
					}
					println(s"[$name] Time to get all decompositions: ${getAllTime._2} us (got ${getAllTime._1})")*/
					name -> (times /*::: List(getAllTime._2)*/)
				}

				val topTimesCsv = topTimesByEnumerator.flatMap(_._2).mkString(",")
				this.synchronized {
					Files.write(
						resultsPath,
						s"${sqlFile.getName.stripSuffix(".sql")},${sqlIR.hyperedges.size},$width,$widthSearchTime,$topTimesCsv\n".getBytes,
						StandardOpenOption.APPEND
					)
				}
			})
		}
	}

	private def timeIt[T](block: => T): (T, Long) = {
		val start = System.nanoTime()
		val result = block
		(result, (System.nanoTime() - start) / 1000)
	}

	private def timeItRepeat[T](block: => T): (T, Long) = {
		val start = System.nanoTime()
		var result: T = null.asInstanceOf[T]
		for (i <- 0 until repeatTimes){
			result = block
		}
		(result, (System.nanoTime() - start) / 1000 / repeatTimes)
	}
}
