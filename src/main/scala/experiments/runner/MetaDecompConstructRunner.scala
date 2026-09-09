package experiments.runner

import decompositions.{Hypergraph, MetaDecompBasedOptimizer, MetaDecompCyclicOptimizer, MetaDecompGraph, MetaDecompGraphConstructBaseline, MetaDecompGraphConstructInterpolatable, MetaDecompGraphConstructor, MetaDecompGraphRandomizedConstructor, metaGYO}
import experiments.Config.{benchmarks, benchmarksPath, repeatTimes, resultsDir, sqlFilesInBenchmark}
import sql.{IR, SQLParser}

import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.sys.process.*
import experiments.parseSubqueryTables
import experiments.getTimestamp

object MetaDecompConstructRunner extends BaseRunner {
	def main(args: Array[String]): Unit = {
		/*val constructor = if args.size == 0 then MetaDecompGraphConstructor() else args(0) match {
			case "main" => {
				println("Choosing MetaDecompGraphConstructor()")
				MetaDecompGraphConstructor()
			}
			case "baseline" => {
				println("Choosing MetaDecompGraphConstructBaseline()")
				MetaDecompGraphConstructBaseline()
			}
			case "interpolatable" => {
				println("Choosing MetaDecompGraphConstructInterpolatable()")
				MetaDecompGraphConstructInterpolatable()
			}
		}*/
		for (benchmark <- if args.size >= 1 then List(args(0)) else benchmarks) {
			connect(benchmark)

			val benchmarkPath = s"$benchmarksPath/$benchmark"
			val resultsPath = Paths.get(resultsDir, s"metadecomp-construct-new-$benchmark-$getTimestamp.csv")
			Files.write(
				resultsPath,
				"query,meta_time,meta_baseline_time,are_equal\n".getBytes,
				StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
			)


			sqlFilesInBenchmark(benchmark).filter(f => if args.size >= 2 then f.getName.matches(args(1)) else true).foreach(sqlFile =>
				val queryName = sqlFile.getName.stripSuffix(".sql")

				if (Files.exists(Paths.get(benchmarkPath, "cardinalities", s"${queryName}.csv"))) {
					System.gc()

					println("\n-----------------------------------")
					println(s"${sqlFile.getName}")


					val source = Source.fromFile(sqlFile)
					val query = source.getLines().mkString(" ")
					source.close()

					implicit val sqlIR: sql.IR = SQLParser.parse(query)

					// toggleOptimizers(sqlIR.outputAttributes.size <= 3)
					if (metaGYO(sqlIR.hyperedges).isEmpty) { // Cyclic

						def hypergraphFromEdges(edgesByIdVertices: List[(String, Set[String | Int])]): Hypergraph = {
							val vertexMap = edgesByIdVertices.flatMap(_._2).map(id => id -> Hypergraph.Vertex(id.toString)).toMap
							val edges = edgesByIdVertices.map { case (id, vertexIds) => Hypergraph.Hyperedge(vertexIds.map(vertexMap(_)), id) }.toSet
							Hypergraph(vertexMap.values.toSet, edges)
						}

						val hypergraph = Hypergraph(sqlIR.hyperedges.flatMap(_.nodes).toSet, sqlIR.hyperedges)

						/*val hypergraph = hypergraphFromEdges(List(
							("a", Set("1", "2")),
							//("b", Set("6")),
							("c", Set("2", "3")),
							//("d", Set("7")),
							//("e", Set("4", "1", "6")),
							("f", Set("3", "4")),
							//("g", Set("4", "5")),
							//("h", Set("0"))
						))*/

						println(hypergraph)

						var width = 2
						var meta: MetaDecompGraph = null
						while ({ meta = MetaDecompGraphRandomizedConstructor().run(hypergraph, width); meta.sortedEdges.isEmpty } ) {
							println(s"Width ${width} failed")
							width += 1
						}
						println(s"Width ${width}")

						var metaBaseline = MetaDecompGraphConstructBaseline().run(hypergraph, width)


						val areEqual = (meta.sortedEdges.toSet == metaBaseline.sortedEdges.toSet)
						println(s"Are equal: $areEqual")

						val metaGraphTime = (for (i <- 0 until repeatTimes) yield {
							val metaStartTime = System.nanoTime()
							var meta = MetaDecompGraphConstructor().run(hypergraph, width)
							val metaEndTime = System.nanoTime()
							val metaTime = (metaEndTime - metaStartTime) / 1000 // microseconds
							println(s"Meta graph construction run $i: $metaTime us")


							metaTime
						}).sorted.apply(repeatTimes / 2) // returns the median

						val metaGraphTimeBaseline = (for (i <- 0 until repeatTimes) yield {
							val metaStartTimeBaseline = System.nanoTime()
							var metaBaseline = MetaDecompGraphConstructBaseline().run(hypergraph, width)
							val metaEndTimeBaseline = System.nanoTime()
							val metaTimeBaseline = (metaEndTimeBaseline - metaStartTimeBaseline) / 1000 // microseconds
							println(s"Meta graph construction run $i: $metaTimeBaseline us (baseline)")


							metaTimeBaseline
						}).sorted.apply(repeatTimes / 2) // returns the median

						println(s"${sqlFile.getName},$metaGraphTime,$metaGraphTimeBaseline,$areEqual")
						Files.write(
							resultsPath,
							s"${sqlFile.getName},$metaGraphTime,$metaGraphTimeBaseline,$areEqual\n".getBytes,
							StandardOpenOption.APPEND
						)

					}
				}
			)
			conn.close()
		}
	}
}
