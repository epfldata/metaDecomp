package experiments.runner

import decompositions.{MetaDecompBasedOptimizer, metaGYO}
import experiments.Config.{benchmarks, benchmarksPath, cardinalityEstimationVariants, repeatTimes, resultsDir, sqlFilesInBenchmark}
import sql.{Attribute, IR, Relation, SQLParser}

import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.io.Source
import scala.jdk.CollectionConverters.*

object MetaDecompRunner extends BaseRunner {
	def main(args: Array[String]): Unit = {
		for (benchmark <- benchmarks ; estimation <- cardinalityEstimationVariants(benchmark)) {

			val benchmarkPath = s"$benchmarksPath/$benchmark"
			val resultsPath = Paths.get(resultsDir, s"metadecomp-$benchmark-$estimation.csv")
			Files.write(
				resultsPath,
				"query, num_rels, max_fanout, metagyo_time, planning_time, total_opt_time, exec_time, total_time\n".getBytes,
				StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
			)


			sqlFilesInBenchmark(benchmark).foreach(sqlFile =>
				println("\n-----------------------------------")
				println(s"${sqlFile.getName}")

				val queryName = sqlFile.getName.stripSuffix(".sql")


				val source = Source.fromFile(sqlFile)
				val query = source.getLines().mkString(" ")
				source.close()

				implicit val sqlIR: sql.IR = SQLParser.parse(query)


				val metaRunResults = for (i <- 0 until repeatTimes) yield {
					val metaGYOStartTime = System.nanoTime()
					val meta = metaGYO(sqlIR.hyperedges)
					val metaGYOEndTime = System.nanoTime()
					val metaGYOTime = (metaGYOEndTime - metaGYOStartTime) / 1000 // microseconds
					println(s"MetaGYO run $i: $metaGYOTime us")
					(meta, metaGYOTime)
				}

				val (meta, metaGYOTime) = metaRunResults.sortBy(_._2).apply(repeatTimes / 2) // Take the median of 5 runs


				val maxFanout = metaRunResults.head._1.collectDescendents.toList.map(p => (p.childrenPlusOrigin ++ p.parent).size - 1).max

				val joinedTablesFileSource = Source.fromFile(Paths.get(benchmarkPath, "cardinalities", "subquery_joined_tables", s"${queryName}_join_tables.txt").toFile)
				val joinedTables = joinedTablesFileSource.getLines

				if (estimation == "all-0") {
					sqlIR.cardinalities = joinedTables.map(tablesLine => {
						val hyperedgeAliasesOnLine = tablesLine.split(",").map(_.trim)
						val hyperedgesOnLine = hyperedgeAliasesOnLine.map(alias => sqlIR.hyperedges.find(_.alias == alias).get)
						hyperedgesOnLine.toSet -> 0L
					}).toMap
				} else {
					val cardinalitiesFileSource = Source.fromFile(Paths.get(benchmarkPath, "cardinalities", estimation, "results", s"${queryName}_cardinalities.txt").toFile)
					val cardinalities = cardinalitiesFileSource.getLines
					sqlIR.cardinalities = joinedTables.zip(cardinalities).map((tablesLine, cardinalitiesLine) =>
						val hyperedgeAliasesOnLine = tablesLine.split(",").map(_.trim)
						val hyperedgesOnLine = hyperedgeAliasesOnLine.map(alias => sqlIR.hyperedges.find(_.alias == alias).get)
						val cardinality = cardinalitiesLine.split(" ").takeRight(1).head.toLong
						hyperedgesOnLine.toSet -> cardinality
					).toMap
					cardinalitiesFileSource.close()
				}

				joinedTablesFileSource.close()

				val (plan, planningTime) = (for (i <- 0 until repeatTimes) yield {
					val planningStartTime = System.nanoTime()
					val plan = MetaDecompBasedOptimizer().run(metaRunResults(i)._1)
					val planningEndTime = System.nanoTime()
					val planningTime = (planningEndTime - planningStartTime) / 1000 // microseconds
					println(s"Planning run $i: $planningTime us")
					(plan, planningTime)
				}).sortBy(_._2).apply(repeatTimes / 2) // Take the median of 5 runs


				val totalOptTime = metaGYOTime + planningTime // microseconds

				println(s"Optimization time: $metaGYOTime us + $planningTime us = $totalOptTime us")


				val jsonFilePath = Paths.get(resultsDir, "join-trees", s"${queryName}_join_tree.json")
				Files.write(jsonFilePath, plan.joinTree.toJson.getBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)

				val executionTime = runPlan(plan)

				println(s"Execution time: $executionTime us")


				val totalTime = totalOptTime + executionTime
				println(s"Total time: ${totalOptTime + executionTime} us")

				Files.write(resultsPath, s"$queryName, ${sqlIR.hyperedges.size}, $maxFanout, $metaGYOTime, $planningTime, $totalOptTime, $executionTime, $totalTime\n".getBytes, StandardOpenOption.APPEND)
			)
		}

		conn.close()
	}
}
