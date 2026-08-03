package experiments.runner

import decompositions.{MetaDecompBasedOptimizer, metaGYO}
import experiments.Config.{benchmarks, benchmarksPath, repeatTimes, resultsDir, sqlFilesInBenchmark}
import sql.{IR, SQLParser}

import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.sys.process._
import experiments.parseSubqueryTables
import experiments.getTimestamp
import decompositions.MetaDecompGraphConstructor
import decompositions.MetaDecompCyclicOptimizer
import decompositions.MetaDecompGraph
import decompositions.Hypergraph

object MetaDecompRunner extends BaseRunner {
	def main(args: Array[String]): Unit = {
		for (benchmark <- if args.size >= 1 then List(args(0)) else benchmarks) {
			connect(benchmark)

			val benchmarkPath = s"$benchmarksPath/$benchmark"
			val resultsPath = Paths.get(resultsDir, s"metadecomp-opt-$benchmark-$getTimestamp.csv")
			Files.write(
				resultsPath,
				"query,num_rels,max_fanout,metagyo_time,planning_time,total_opt_time,exec_time,total_time,cost_intm,cost_in,cout_cost\n".getBytes,
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

						val hypergraph = Hypergraph(sqlIR.hyperedges.flatMap(_.nodes).toSet, sqlIR.hyperedges)

						println(hypergraph)

						var width = 2
						var meta: MetaDecompGraph = null
						while ({ meta = MetaDecompGraphConstructor().run(hypergraph, width); meta.sortedEdges.isEmpty } ) {
							println(s"Width ${width} failed")
							width += 1
						}
						println(s"Width ${width}")

						val metaGraphTime = (for (i <- 0 until repeatTimes) yield {
							val metaStartTime = System.nanoTime()
							MetaDecompGraphConstructor().run(hypergraph, width)
							val metaEndTime = System.nanoTime()
							val metaTime = (metaEndTime - metaStartTime) / 1000 // microseconds
							println(s"Meta graph construction run $i: $metaTime us")
							metaTime
						}).sorted.apply(repeatTimes / 2)
						
						// println(meta)
						// println(meta.sortedEdges)

						println("Loading cardinalities...")

						val joinedTablesFileSource = Source.fromFile(Paths.get(benchmarkPath, "cardinalities", s"${queryName}.csv").toFile)
						val joinedTables = parseSubqueryTables(joinedTablesFileSource.getLines)

						val cardinalitiesFileSource = Source.fromFile(Paths.get(benchmarkPath, "cardinalities", s"${queryName}.csv").toFile)
						val cardinalities = cardinalitiesFileSource.getLines.drop(3)
						sqlIR.cardinalities = joinedTables.zip(cardinalities).map((tablesLine, cardinalitiesLine) =>
							val hyperedgeAliasesOnLine = tablesLine
							val hyperedgesOnLine = hyperedgeAliasesOnLine.map(alias => sqlIR.hyperedges.find(_.alias == alias).get)
							val cardinality = cardinalitiesLine.split(" ").takeRight(1).head.toDouble
							hyperedgesOnLine.toSet -> cardinality
						).toMap
						cardinalitiesFileSource.close()

						joinedTablesFileSource.close()

						val (plan, planningTime) = (for (i <- 0 until repeatTimes) yield {
							val planningStartTime = System.nanoTime()
							val plan = MetaDecompCyclicOptimizer().run(hypergraph, meta)
							val planningEndTime = System.nanoTime()
							val planningTime = (planningEndTime - planningStartTime) / 1000 // microseconds
							println(s"Planning run $i: $planningTime us")
							(plan, planningTime)
						}).sortBy(_._2).apply(repeatTimes / 2) // Take the median of 5 runs

						val totalOptTime = metaGraphTime + planningTime // microseconds

						println(s"Optimization time: $metaGraphTime us + $planningTime us = $totalOptTime us")

						// println(plan)

						val (viewSql, finalSql, groupBy) = plan.generateSqlWithViews()
						// println(Seq(viewSql, finalSql, groupBy).mkString("\n"))

						val metaPlanSqlFilePath = Paths.get(resultsDir, "meta_plans", s"${queryName}_meta_plan.sql")
						Files.write(metaPlanSqlFilePath, Seq(viewSql, finalSql, groupBy).mkString("\n").getBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)

						val metaPlanDotFilePath = Paths.get(resultsDir, "meta_plans", s"${queryName}_meta_plan.dot")
						Files.write(metaPlanDotFilePath, plan.toDot.getBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)

						val metaPlanFigPath = Paths.get(resultsDir, "meta_plans", s"${queryName}_meta_plan.svg")
						val metaPlanDotCommand = Seq("dot", "-Tsvg", metaPlanDotFilePath.toString, "-o", metaPlanFigPath.toString)
						try {
							metaPlanDotCommand.!
						} catch {
							case e: Throwable => println(s"Error drawing the query plan using dot: ${e.getMessage}")
						}


						val executionTime = runPlan(plan)

						println(s"Execution time: $executionTime us")


						val totalTime = totalOptTime + executionTime
						println(s"Total time: ${totalOptTime + executionTime} us")

						val intermediateCost = plan.intermediateCost
						val inCost = plan.inputCost
						val totalCost = intermediateCost + inCost

						Files.write(
							resultsPath,
							s"$queryName,${sqlIR.hyperedges.size},,$metaGraphTime,$planningTime,$totalOptTime,$executionTime,$totalTime,$intermediateCost,$inCost,$totalCost\n"
								.getBytes,
							StandardOpenOption.APPEND
						)

					} else {
						println(sqlFile.getName())

						val metaRunResults = (for (i <- 0 until repeatTimes) yield {
							val metaGYOStartTime = System.nanoTime()
							val meta = metaGYO(sqlIR.hyperedges)
							val metaGYOEndTime = System.nanoTime()
							val metaGYOTime = (metaGYOEndTime - metaGYOStartTime) / 1000 // microseconds
							println(s"MetaGYO run $i: $metaGYOTime us")
							(meta, metaGYOTime)
						}).sortBy(_._2)

						val (metaOption, metaGYOTime) =
							if metaRunResults.size % 2 == 1 then
								metaRunResults.apply(metaRunResults.size / 2)
							else
								val mid = metaRunResults.size / 2
								(metaRunResults(mid)._1, (metaRunResults(mid)._2 + metaRunResults(mid + 1)._2) / 2)

						val meta = metaOption.get

						val maxFanout = meta.collectDescendents.toList.map(p => (p.childrenPlusOrigin ++ p.parent).size - 1).max



						val joinedTablesFileSource = Source.fromFile(Paths.get(benchmarkPath, "cardinalities", s"${queryName}.csv").toFile)
						val joinedTables = parseSubqueryTables(joinedTablesFileSource.getLines)

						val cardinalitiesFileSource = Source.fromFile(Paths.get(benchmarkPath, "cardinalities", s"${queryName}.csv").toFile)
						val cardinalities = cardinalitiesFileSource.getLines.drop(3)
						sqlIR.cardinalities = joinedTables.zip(cardinalities).map((tablesLine, cardinalitiesLine) =>
							val hyperedgeAliasesOnLine = tablesLine
							val hyperedgesOnLine = hyperedgeAliasesOnLine.map(alias => sqlIR.hyperedges.find(_.alias == alias).get)
							val cardinality = cardinalitiesLine.split(" ").takeRight(1).head.toDouble
							hyperedgesOnLine.toSet -> cardinality
						).toMap
						cardinalitiesFileSource.close()

						joinedTablesFileSource.close()

						val (plan, planningTime) = (for (i <- 0 until repeatTimes) yield {
							val planningStartTime = System.nanoTime()
							val plan = MetaDecompBasedOptimizer().run(metaRunResults(i)._1.get)
							val planningEndTime = System.nanoTime()
							val planningTime = (planningEndTime - planningStartTime) / 1000 // microseconds
							println(s"Planning run $i: $planningTime us")
							(plan, planningTime)
						}).sortBy(_._2).apply(repeatTimes / 2) // Take the median of 5 runs


						val totalOptTime = metaGYOTime + planningTime // microseconds

						println(s"Optimization time: $metaGYOTime us + $planningTime us = $totalOptTime us")


						val jsonFilePath = Paths.get(resultsDir, "join-trees", s"${queryName}_join_tree.json")
						Files.write(
							jsonFilePath,
							plan.joinTree.toJson.getBytes,
							StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
						)

						val executionTime = runPlan(plan)

						println(s"Execution time: $executionTime us")


						val totalTime = totalOptTime + executionTime
						println(s"Total time: ${totalOptTime + executionTime} us")

						val intermediateCost = plan.intermediateCost
						val inCost = plan.inputCost
						val totalCost = intermediateCost + inCost

						Files.write(
							resultsPath,
							s"$queryName,${sqlIR.hyperedges.size},$maxFanout,$metaGYOTime,$planningTime,$totalOptTime,$executionTime,$totalTime,$intermediateCost,$inCost,$totalCost\n"
								.getBytes,
							StandardOpenOption.APPEND
						)
					}
				}
			)
			conn.close()
		}
	}
}
