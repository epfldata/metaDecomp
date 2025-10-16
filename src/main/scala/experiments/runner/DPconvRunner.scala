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

	private val optTimeout = 5.minutes

	def main(args: Array[String]): Unit = {
		for (benchmark <- benchmarks) {
			connect(benchmark)

			implicit val benchmarkPath: String = s"$benchmarksPath/$benchmark"

			val resultsCsvPath = Paths.get(resultsDir, s"dpconv-opt-$benchmark-$getTimestamp.csv")

			Files.write(
				resultsCsvPath,
				s"query,num_rels,opt_time,exec_time,total_time,cout_opt_intm,opt_exec_time\n".getBytes,
				StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
			)

			sqlFilesInBenchmark(benchmark).foreach(sqlFile => {
				val queryName = sqlFile.getName.stripSuffix(".sql")
				val queryCsvPath = s"$benchmarkPath/cardinalities/$queryName.csv"

				if (Files.exists(Paths.get(queryCsvPath))) {
					System.gc()

					println("\n-----------------------------------")
					println(s"${sqlFile.getName}")

					val query = readInOneLine(sqlFile)

					implicit val sqlIR: sql.IR = SQLParser.parse(query)
					
					toggleOptimizers(sqlIR.outputAttributes.size <= 3)


					val (planInString, optTime, optCcap) = runDPConv(queryCsvPath)

					println(s"Optimization time: $optTime us")

					val executionTime = if (planInString.nonEmpty) {
						val plan = parsePlan(planInString)
						plan.projectTo = sqlIR.outputAttributes

						val (viewSqls, finalSql, groupBy) = plan.generateSqlWithViews()

						runPlan(plan)
					} else 0

					println(s"Execution time: $executionTime us")

					val totalTime = optTime + executionTime
					println(s"Total time: ${optTime + executionTime} us")

					val (optPlanInString, _, optCout) = runDPConv(queryCsvPath, capped = false)

					val optPlanExecutionTime = if (optPlanInString.nonEmpty) {
						val plan = parsePlan(optPlanInString)
						plan.projectTo = sqlIR.outputAttributes

						val (viewSqls, finalSql, groupBy) = plan.generateSqlWithViews()

						runPlan(plan)
					} else 0

					Files.write(
						resultsCsvPath,
						s"${sqlFile.getName.stripSuffix(".sql")},${sqlIR.hyperedges.size},$optTime,$executionTime,$totalTime,$optCout,$optPlanExecutionTime\n".getBytes,
						StandardOpenOption.APPEND
					)
				}

			})
			conn.close()
		}
	}

	private def runDPConv(queryCsvPath: String, capped: Boolean = true): (String, Long, Double) = {
		val command = Seq(
			"gtimeout", s"${optTimeout.toSeconds}s", "bash", "-c", s"$dpConvBasePath/src/build/bench $queryCsvPath ${if capped then 1 else 0}"
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
			val coutLinePattern = if capped then """.*optimal_ccap_cost=(.*)""".r else """.*optimal_cout_cost=(.*)""".r
			val optTime = stderrLines.collectFirst { case timeLinePattern(time) => time.toLong }.getOrElse(0L)
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
