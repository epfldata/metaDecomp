package experiments.runner

import decompositions.{JoinNode, PlanNode, ScanNode}
import experiments.Config.*
import experiments.readInOneLine
import sql.{IR, SQLParser}

import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.collection.mutable
import scala.concurrent.duration.*
import scala.sys.process.*
import experiments.getTimestamp
import scala.io.Source
import experiments.parseSubqueryTables

object DPconvRunner extends BaseRunner {
	private val dpConvBasePath = s"$projectRootPath/DPConv"

	private val optTimeout = 1.hour

	def main(args: Array[String]): Unit = {
		for (benchmark <- benchmarks; estimation <- cardinalityEstimationVariants(benchmark)) {
			connect(benchmark)
			disableDuckDBOptimizers()

			implicit val benchmarkPath: String = s"$benchmarksPath/$benchmark"

			val resultsCsvPath = Paths.get(resultsDir, s"dpconv-opt-$benchmark-$estimation-$getTimestamp.csv")

			Files.write(
				resultsCsvPath,
				s"query,num_rels,opt_time,exec_time,total_time${if benchmark == "job-original" then ",cout_opt_intm" else ""}\n".getBytes,
				StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
			)

			sqlFilesInBenchmark(benchmark).foreach(sqlFile => {
				val queryName = sqlFile.getName.stripSuffix(".sql")
				val queryCsvPath = s"$benchmarkPath/cardinalities/$estimation/$queryName.csv"

				if (Files.exists(Paths.get(queryCsvPath))) {
					System.gc()

					println("\n-----------------------------------")
					println(s"${sqlFile.getName}")

					val query = readInOneLine(sqlFile)

					implicit val sqlIR: sql.IR = SQLParser.parse(query)
					

					val joinedTablesFileSource = Source.fromFile(Paths.get(benchmarkPath, "cardinalities", estimation, s"${queryName}.csv" /* "subqueries_tables", s"${queryName}_subqueries_tables.csv" */).toFile)
						val joinedTables = parseSubqueryTables(joinedTablesFileSource.getLines)

						if (estimation == "all-0") {
							sqlIR.cardinalities = joinedTables.map(tablesLine => {
								val hyperedgeAliasesOnLine = tablesLine
								val hyperedgesOnLine = hyperedgeAliasesOnLine.map(alias => sqlIR.hyperedges.find(_.alias == alias).get)
								hyperedgesOnLine.toSet -> 0.0
							}).toMap
						} else {
							val cardinalitiesFileSource = Source.fromFile(Paths.get(benchmarkPath, "cardinalities", estimation, /* "dpconv_input", */ s"${queryName}.csv").toFile)
							val cardinalities = cardinalitiesFileSource.getLines.drop(3)
							sqlIR.cardinalities = joinedTables.zip(cardinalities).map((tablesLine, cardinalitiesLine) =>
								val hyperedgeAliasesOnLine = tablesLine
								val hyperedgesOnLine = hyperedgeAliasesOnLine.map(alias => sqlIR.hyperedges.find(_.alias == alias).get)
								val cardinality = cardinalitiesLine.split(" ").takeRight(1).head.toDouble
								hyperedgesOnLine.toSet -> cardinality
							).toMap
							cardinalitiesFileSource.close()
						}

						joinedTablesFileSource.close()


					val (planInString, optTime, optCout) = runDPConv(queryCsvPath)

					println(s"Optimization time: $optTime us")

					val executionTime = if (planInString.nonEmpty) {
						val plan = parsePlan(planInString)
						plan.projectTo = sqlIR.outputAttributes

						val (viewSqls, finalSql, groupBy) = plan.generateSqlWithViews()

						val dpConvPlanSqlPath = Paths.get(resultsDir, "dpconv_plans", s"${queryName}_dpconv_plan.sql")
						Files.write(dpConvPlanSqlPath, (viewSqls + "\n" + finalSql + "\n" + groupBy + ";").getBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)

						val dpConvPlanDotFilePath = Paths.get(resultsDir, "dpconv_plans", s"${queryName}_dpconv_plan.dot")
						Files.write(dpConvPlanDotFilePath, plan.toDot.getBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)

						val dpConvPlanFigPath = Paths.get(resultsDir, "dpconv_plans", s"${queryName}_dpconv_plan.svg")
						val dpConvPlanDotCommand = Seq("dot", "-Tsvg", dpConvPlanDotFilePath.toString, "-o", dpConvPlanFigPath.toString)
						try {
							dpConvPlanDotCommand.!
						} catch {
							case e: Throwable => System.err.println(s"Error generating query plan for $queryName: ${e.getMessage}")
						}

						runPlan(plan)
					} else 0

					println(s"Execution time: $executionTime us")

					val totalTime = optTime + executionTime
					println(s"Total time: ${optTime + executionTime} us")

					Files.write(
						resultsCsvPath,
						s"${sqlFile.getName.stripSuffix(".sql")},${sqlIR.hyperedges.size},$optTime,$executionTime,$totalTime${if benchmark == "job-original" then s",$optCout" else ""}\n".getBytes,
						StandardOpenOption.APPEND
					)
				}

			})
			conn.close()
		}
	}

	private def runDPConv(queryCsvPath: String): (String, Long, Double) = {
		val command = Seq(
			"gtimeout", s"${optTimeout.toSeconds}s", "bash", "-c", s"$dpConvBasePath/src/build/bench $queryCsvPath"
		)

		val results = (for (i <- 0 until repeatTimes) yield {
			println(s"Started DPconv run $i at $getTimestamp")

			val stdoutLines = mutable.ListBuffer[String]()
			val stderrLines = mutable.ListBuffer[String]()

			val exitCode = command.!(ProcessLogger(fout = stdoutLines.addOne, ferr = stderrLines.addOne))

			println(stdoutLines.mkString("\n"))
			println(stderrLines.mkString("\n"))

			if (exitCode != 0) {
				println("DPconv timed out or crashed.")
				if (i == 0) {
					return ("", optTimeout.toMicros, 0L)
				}
			}

			val timeLinePattern = """CCAP_DPCONV \(time\): (\d+)""".r
			val coutLinePattern = """.*optimal_cout_cost=(.*)""".r
			val optTime = stderrLines.collectFirst { case timeLinePattern(time) => time.toLong }.get
			val optCout = stderrLines.collectFirst { case coutLinePattern(cout) => cout.toDouble }.getOrElse(0.0)
			println(s"Optimization time (run $i): $optTime us")
			(stderrLines.findLast(_.startsWith("(")).get, optTime, optCout)
		}).sortBy(_._2)

		if repeatTimes % 2 == 0 then {
			(results(repeatTimes / 2)._1, (results(repeatTimes / 2 - 1)._2 + results(repeatTimes / 2)._2) / 2, results(repeatTimes / 2)._3)
		} else {
			results(repeatTimes / 2)
		}
	}

	private def parsePlan[V](plan: String)(implicit sqlIR: sql.IR): PlanNode = {
		val leafPattern = """\((\w+)\)""".r

		def parseRecursive(subPlan: String): PlanNode = {
			val trimmed = subPlan.trim
			if (leafPattern.matches(trimmed)) {
				val hyperedge = sqlIR.hyperedges.find(_.alias == trimmed.drop(1).dropRight(1)).get
				ScanNode(hyperedge) // Replace with actual HyperEdge creation logic
			} else {
				// Internal node
				val innerPlan = trimmed.drop(1).dropRight(1) // Remove outer parentheses
				val splitIndex = findSplitIndex(innerPlan)
				val left = innerPlan.substring(0, splitIndex).trim
				val right = innerPlan.substring(splitIndex + 1).trim
				JoinNode(parseRecursive(left), parseRecursive(right))
			}
		}

		def findSplitIndex(plan: String): Int = {
			var balance = 0
			for (i <- plan.indices) {
				plan(i) match {
					case '(' => balance += 1
					case ')' => balance -= 1
					case '|' if balance == 0 => return i
					case _ => ()
				}
			}
			throw new IllegalArgumentException("Invalid plan format")
		}

		parseRecursive(plan)
	}
}
