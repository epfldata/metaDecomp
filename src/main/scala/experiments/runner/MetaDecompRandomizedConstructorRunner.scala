package experiments.runner

import sql.{IR, SQLParser}
import decompositions.{Hypergraph, KDecomp, MetaDecompBasedOptimizer, MetaDecompCyclicOptimizer, SharedWorkCyclicOptimizer,  MetaDecompGraph, MetaDecompGraphConstructBaseline, MetaDecompGraphConstructInterpolatable, MetaDecompGraphConstructor, PlanNode, metaGYO}
import decompositions.Hypergraph.{Component, Hyperedge, HyperedgeSetExtension, Separator, Vertex}
import utils.*

import scala.collection.mutable
import scala.util.control.Breaks.{break, breakable}
import scala.collection.immutable.HashSet
import scala.util.Random
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.sys.process.*
import dotty.tools.dotc.util.SimpleIdentitySet.empty
import experiments.Config.{benchmarks, benchmarksPath, repeatTimes, resultsDir, sqlFilesInBenchmark}

import java.nio.file.{Files, Paths, StandardOpenOption}
import experiments.{getTimestamp, parseSubqueryTables}
import experiments.runner.BaseRunner
import experiments.runner.MetaDecompRunner.connect


def getWidth(hypergraph: Hypergraph): Int = {
  var width = 1

  while ( {
    !KDecomp().run(hypergraph, width)
  }) {
    width += 1
  }
  width
}


object MetaDecompRandomizedConstructorRunner extends experiments.runner.BaseRunner {

  class Hypertree(val root: Separator) {
    var children = mutable.Map.empty[Component, Hypertree]
  }

  // constructs MetaDecompConsisting only of one decomposition
  private def HypertreeToMetaDecompGraph(tree: Hypertree, vertices: Set[Hypergraph.Vertex]) : MetaDecompGraph = {
    val graph = new MetaDecompGraph()

    def addGraphEdges(prevSep: Separator, component: Component, subtree: Hypertree): Unit = {
      graph.addEdge(prevSep, subtree.root, component)
      subtree.children.foreach { case (c, s) => addGraphEdges(subtree.root, c, s) }
    }
    val emptySeparator = Set.empty[Hyperedge]
    addGraphEdges(emptySeparator, vertices, tree)
    graph
  }

  def run(hypergraph: Hypergraph, width: Int, benchmarkPath: String = "", queryName: String = "", sqlFile: java.io.File, query: String, fairMeasurement: Boolean = false): String = {
    val startTime = System.nanoTime()
    var excludedMeasurementTime = 0L
    def timedOut = (System.nanoTime() - startTime - excludedMeasurementTime) / 1000 > 7500000 * math.pow(hypergraph.edges.size, 2) * math.pow(5, width - 2)
    // 500 * hypergraph.edges.size * math.pow(hypergraph.edges.size / 4, width - 2)

    def excludeFromConstructionTimeout[A](operation: => A): A = {
      val measurementStart = System.nanoTime()
      try operation
      finally excludedMeasurementTime += System.nanoTime() - measurementStart
    }

    val unexhaustedCandidates = mutable.Map.empty[(Separator, Separator, Component), mutable.ListBuffer[Separator]]
    val hasSolution = mutable.Map.empty[(Separator, Separator), Boolean]
    val unaddedSolution = mutable.Map.empty[(Separator, Separator), Hypertree]
    // To store subtrees that are part of a failed run, i.e., not yet added to the meta-decomposition graph, to be used if the same subproblem is encountered again.
    val exhausted = mutable.Set.empty[(Separator, Separator)] // Subproblems of which all possible descendents have been explored.
    val componentExhausted = mutable.Set.empty[(Separator, Component)]
    val componentHasSolution = mutable.Map.empty[(Separator, Component), Boolean]
    // Returned trees are also planned independently, so cached successes must
    // retain their descendants even after their edges enter the shared graph.
    val completeSolutions = mutable.Map.empty[(Separator, Separator), Hypertree]
    val componentSolutions = mutable.Map.empty[(Separator, Component), Hypertree]

    val graph = new MetaDecompGraph()
    val H = hypergraph

    println("Loading cardinalities...")

    val joinedTablesFileSource = Source.fromFile(Paths.get(benchmarkPath, "cardinalities", s"${queryName}.csv").toFile)
    val joinedTables = parseSubqueryTables(joinedTablesFileSource.getLines)

    val cardinalitiesFileSource = Source.fromFile(Paths.get(benchmarkPath, "cardinalities", s"${queryName}.csv").toFile)
    val cardinalities = cardinalitiesFileSource.getLines.drop(3)


    implicit val sqlIR: sql.IR = SQLParser.parse(query)
    sqlIR.cardinalities = joinedTables.zip(cardinalities).map((tablesLine, cardinalitiesLine) =>
      val hyperedgeAliasesOnLine = tablesLine
      val hyperedgesOnLine = hyperedgeAliasesOnLine.map(alias => sqlIR.hyperedges.find(_.alias == alias).get)
      val cardinality = cardinalitiesLine.split(" ").takeRight(1).head.toDouble
      hyperedgesOnLine.toSet -> cardinality
    ).toMap
    cardinalitiesFileSource.close()

    joinedTablesFileSource.close()

    println("Done")

    def getAndExecutePlan(meta: MetaDecompGraph, label: String): (Long, Double) = {
      val planningStartTime = System.nanoTime()
      val plan = SharedWorkCyclicOptimizer().run(hypergraph, meta)
      val planningTime = (System.nanoTime() - planningStartTime) / 1000
      val cost = plan.cumulativeCost
      println(s"$label: planning=$planningTime us, cumulativeCost=$cost")
      (runPlan(plan), cost)
    }

    case class InsertedCandidate(insertion: Int, plan: PlanNode, preliminaryTime: Long, cost: Double)

    val insertedCandidates = mutable.ArrayBuffer.empty[InsertedCandidate]

    def median(times: Seq[Long]): Long = times.sorted.apply(times.size / 2)

    /**
     * Preliminary comparison for the newly inserted tree. Each plan runs the
     * same number of times in first and second position.
     */
    def measureAlternating(accumulatedPlan: PlanNode, insertedPlan: PlanNode): (Long, Long) = {
      val accumulatedTimes = mutable.ArrayBuffer.empty[Long]
      val insertedTimes = mutable.ArrayBuffer.empty[Long]

      for (round <- 0 until repeatTimes) {
        if (round % 2 == 0) {
          accumulatedTimes += runPlanOnce(accumulatedPlan)
          insertedTimes += runPlanOnce(insertedPlan)
        } else {
          insertedTimes += runPlanOnce(insertedPlan)
          accumulatedTimes += runPlanOnce(accumulatedPlan)
        }
      }

      (median(accumulatedTimes.toSeq), median(insertedTimes.toSeq))
    }

    /**
     * Independently remeasure the accumulated plan and the three inserted
     * candidates with the lowest preliminary medians. One unmeasured run per
     * plan warms the execution path. Rotating the starting plan gives every
     * plan every execution position equally often.
     */
    def remeasureFinalists(accumulatedPlan: PlanNode, finalists: Seq[InsertedCandidate]): (Long, Seq[(InsertedCandidate, Long)]) = {
      val labelledPlans = ("accumulated", accumulatedPlan) +:
        finalists.map(candidate => s"inserted-${candidate.insertion}" -> candidate.plan)

      labelledPlans.foreach { case (_, plan) => runPlanOnce(plan) }

      val times = mutable.Map.from(labelledPlans.map { case (label, _) => label -> mutable.ArrayBuffer.empty[Long] })
      val planCount = labelledPlans.size
      val rounds = ((repeatTimes + planCount - 1) / planCount) * planCount

      for (round <- 0 until rounds) {
        val offset = round % planCount
        val executionOrder = labelledPlans.drop(offset) ++ labelledPlans.take(offset)
        executionOrder.foreach { case (label, plan) =>
          times(label) += runPlanOnce(plan)
        }
      }

      val accumulatedTime = median(times("accumulated").toSeq)
      val insertedTimes = finalists.map(candidate =>
        candidate -> median(times(s"inserted-${candidate.insertion}").toSeq)
      )
      (accumulatedTime, insertedTimes)
    }

    def measureSinglePlan(plan: PlanNode): Long = {
      runPlanOnce(plan) // unmeasured warm-up
      median((0 until repeatTimes).map(_ => runPlanOnce(plan)))
    }

    def rec(prevSep: Separator, currComp: Set[Vertex], currSep: Separator, depth: Int)(implicit width: Int): Option[Hypertree] = {
      if (unaddedSolution.contains((prevSep, currSep))) {
        return Some(unaddedSolution((prevSep, currSep)))
      } else if (exhausted.contains((prevSep, currSep)) && hasSolution((prevSep, currSep))) {
        return Some(completeSolutions((prevSep, currSep)))
      }
      if (!hasSolution.getOrElse((prevSep, currSep), true)) {
        return None
      }

      if (timedOut) return None

      val separatorNodes = currSep.nodes // hoisted: avoid recomputing per component / per candidate

      val components = H.componentsInducedBy(separatorNodes).filter(_.subsetOf(currComp))
      var allExhausted = true
      val tree = Hypertree(currSep)
      if (components.exists(c => componentHasSolution.getOrElse((currSep, c), true) == false)) {
        hasSolution((prevSep, currSep)) = false
        return None
      }
      components.foreach { nextComp =>
        if (timedOut) return None
        var successful = false
        val candidates = unexhaustedCandidates.getOrElseUpdate((prevSep, currSep, nextComp), H.enumNextSeparatorCandidates(currSep, nextComp, width))

        breakable { while (candidates.nonEmpty) {
          if (timedOut) return None
          val i = Random.nextInt(candidates.size)
          val nextSep = candidates(i)
          rec(currSep, nextComp, nextSep, depth + 1) match {
            case Some(subtree) =>
              successful = true
              tree.children += (nextComp, subtree)
              componentHasSolution((currSep, nextComp)) = true
              componentSolutions((currSep, nextComp)) = subtree
              if (exhausted.contains((currSep, nextSep))) {
                candidates.filterInPlace(!nextSep.subsetOf(_))
              }
              break()
            case None =>
              candidates.remove(i)
            // Continue to try the next S'
          }
        } }
        if (candidates.nonEmpty) {
          allExhausted = false
        } else {
          componentExhausted += ((currSep, nextComp))
          if (componentHasSolution.getOrElse((currSep, nextComp), false) == false) {
            // If known to have no solution, or no solution is added at all
            hasSolution((prevSep, currSep)) = false
            componentHasSolution((currSep, nextComp)) = false
            return None
          }
          if (componentHasSolution.getOrElse((currSep, nextComp), false) == true) {
            successful = true
            tree.children.getOrElseUpdate(nextComp, componentSolutions((currSep, nextComp)))
          }
        }

        if (!successful) { // Some component is not successful
          hasSolution((prevSep, currSep)) = false
          return None
        }
      }
      hasSolution((prevSep, currSep)) = true
      unaddedSolution((prevSep, currSep)) = tree
      completeSolutions((prevSep, currSep)) = tree
      if (allExhausted) {
        exhausted += ((prevSep, currSep))
      }
      Some(tree)
    }

    def addGraphEdges(prevSep: Separator, component: Component, subtree: Hypertree): Unit = {
      graph.addEdge(prevSep, subtree.root, component)
      unaddedSolution.remove((prevSep, subtree.root))
      subtree.children.foreach { case (c, s) => addGraphEdges(subtree.root, c, s) }
    }

    // iterate over all possible roots
    val possibleRoots = H.edges.subsetsOfSizeAtMost(width).filter(_.nonEmpty).toIndexedSeq
    val emptySeparator = Set.empty[Hyperedge]

    var cnt = 0
    val cntLimit = 20
    var extractedSeq = ""
    var lowestInsertedTime: Long = -1
    var lowestInsertedCost = Double.PositiveInfinity
    var fastestInsertedCost = Double.NaN
    var previousCompleteCost: Option[Double] = None

    def processTree(tree: Hypertree): Unit = {
      addGraphEdges(emptySeparator, H.vertices, tree)
      cnt += 1
      val extracted = graph.countHypertreeDecompositions()
      extractedSeq += s",$extracted"

      val (completeTime, completeCost, insertedTreeTime, insertedCost, selectedInsertedCost) =
        if (fairMeasurement) excludeFromConstructionTimeout {
          val accumulatedPlanningStart = System.nanoTime()
          val accumulatedPlan = SharedWorkCyclicOptimizer().run(hypergraph, graph)
          val accumulatedPlanningTime = (System.nanoTime() - accumulatedPlanningStart) / 1000

          val insertedGraph = HypertreeToMetaDecompGraph(tree, H.vertices)
          val insertedPlanningStart = System.nanoTime()
          val insertedPlan = SharedWorkCyclicOptimizer().run(hypergraph, insertedGraph)
          val insertedPlanningTime = (System.nanoTime() - insertedPlanningStart) / 1000
          val insertedCost = insertedPlan.cumulativeCost

          println(s"Accumulated graph: planning=$accumulatedPlanningTime us, cumulativeCost=${accumulatedPlan.cumulativeCost}")
          println(s"Inserted tree: planning=$insertedPlanningTime us, cumulativeCost=$insertedCost")

          val (_, preliminaryInsertedTime) = measureAlternating(accumulatedPlan, insertedPlan)
          insertedCandidates += InsertedCandidate(cnt, insertedPlan, preliminaryInsertedTime, insertedCost)

          val finalists = insertedCandidates.sortBy(_.preliminaryTime).take(3).toSeq
          val (fairAccumulatedTime, finalistTimes) = remeasureFinalists(accumulatedPlan, finalists)
          val (fastestCandidate, fairInsertedTime) = finalistTimes.minBy(_._2)

          println(s"Preliminary inserted time: $preliminaryInsertedTime us")
          println(s"Remeasured finalists: ${finalistTimes.map { case (candidate, time) => s"insertion ${candidate.insertion}=$time us" }.mkString(", ")}")

          (fairAccumulatedTime, accumulatedPlan.cumulativeCost, fairInsertedTime, insertedCost, fastestCandidate.cost)
        } else {
          val (accumulatedTime, accumulatedCost) = getAndExecutePlan(graph, "Accumulated graph")
          val (newInsertedTime, newInsertedCost) = getAndExecutePlan(HypertreeToMetaDecompGraph(tree, H.vertices), "Inserted tree")
          (accumulatedTime, accumulatedCost, newInsertedTime, newInsertedCost, newInsertedCost)
        }

      if (fairMeasurement) {
        // This value is independently remeasured at every insertion; do not
        // retain a historical minimum measured under an earlier cache state.
        lowestInsertedTime = insertedTreeTime
        fastestInsertedCost = selectedInsertedCost
      } else if (lowestInsertedTime == -1 || lowestInsertedTime > insertedTreeTime) {
          lowestInsertedTime = insertedTreeTime
          fastestInsertedCost = insertedCost
        }
      lowestInsertedCost = math.min(lowestInsertedCost, insertedCost)

      println(s"$queryName insertion=$cnt: completeTime=$completeTime us, insertedTime=$insertedTreeTime us, lowestInsertedTime=$lowestInsertedTime us")
      println(s"Costs: complete=$completeCost, inserted=$insertedCost, lowestInserted=$lowestInsertedCost, fastestInserted=$fastestInsertedCost")
      def materiallyHigher(value: Double, reference: Double): Boolean =
        value > reference + 1e-9 * math.max(1.0, math.abs(reference))
      if (materiallyHigher(completeCost, lowestInsertedCost)) {
        println("Cost comparison: accumulated plan costs more than the cheapest inserted plan; investigate optimizer selection.")
      }
      previousCompleteCost.foreach { previous =>
        if (materiallyHigher(completeCost, previous)) {
          println(s"Cost comparison: accumulated plan cost increased from $previous to $completeCost after adding a tree.")
        }
      }
      previousCompleteCost = Some(completeCost)

      // Each insertion contributes: HD count, best inserted time, accumulated
      // time, best inserted cost, accumulated cost (times are microseconds).
      extractedSeq += s",$lowestInsertedTime,$completeTime,$lowestInsertedCost,$completeCost"
      //println(extractedSeq)
    }

    breakable { for (root <- possibleRoots) {
      if (timedOut || (cnt >= cntLimit)) break
      val tree = new Hypertree(root)
      rec(emptySeparator, H.vertices, root, 1)(width) match {
        case Some(tree) => {
          processTree(tree)
        }
        case None => // continue
      }
    } }

    val incompleteRoots = mutable.ListBuffer.from(possibleRoots.filterNot(s => exhausted.contains((emptySeparator, s))))

    while (!timedOut && incompleteRoots.nonEmpty && (cnt < cntLimit)) {
      val i = Random.nextInt(incompleteRoots.size)
      val root = incompleteRoots(i)
      rec(emptySeparator, H.vertices, root, 1)(width) match {
        case Some(tree) =>
          processTree(tree)
          if (exhausted.contains((emptySeparator, root))) {
            incompleteRoots.remove(i)
          }
        case None => incompleteRoots.remove(i)
      }
    }

    extractedSeq += ",complete"
    val completeGraph = MetaDecompGraphConstructBaseline().run(hypergraph, width)
    val (execTime, cost) =
      if (fairMeasurement) {
        val planningStart = System.nanoTime()
        val plan = MetaDecompCyclicOptimizer().run(hypergraph, completeGraph)
        val planningTime = (System.nanoTime() - planningStart) / 1000
        //println(s"Complete: planning=$planningTime us, cumulativeCost=${plan.cumulativeCost}")
        (measureSinglePlan(plan), plan.cumulativeCost)
      } else {
        getAndExecutePlan(completeGraph, "Complete")
      }
    val extractedHDs = completeGraph.countHypertreeDecompositions()
    extractedSeq += s",$execTime,$cost"

    if (extractedSeq != "") {
      println(extractedSeq)
    }
    println(s"$queryName,$extractedHDs" )
    return extractedSeq
  }

  def main(args: Array[String]): Unit = {
    for (benchmark <- if args.size >= 1 then List(args(0)) else benchmarks) {
      connect(benchmark)

      val benchmarkPath = s"$benchmarksPath/$benchmark"
      val resultsPath = Paths.get(resultsDir, s"metadecomp-randomized-progress-$benchmark-$getTimestamp.csv")
      Files.write(
        resultsPath,
        "".getBytes,
        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
      )
      for (_ <- 0 until 1) {

        sqlFilesInBenchmark(benchmark).filter(f => if args.size >= 2 then f.getName.matches(args(1)) else true).foreach(sqlFile =>
          val queryName = sqlFile.getName.stripSuffix(".sql")

          val source = Source.fromFile(sqlFile)
          val query = source.getLines().mkString(" ")
          source.close()

          implicit val sqlIR: sql.IR = SQLParser.parse(query)
          val hypergraph = Hypergraph(sqlIR.hyperedges.flatMap(_.nodes).toSet, sqlIR.hyperedges)

          val width = getWidth(hypergraph)

          val extractedSeq: String = run(hypergraph, width, benchmarkPath, queryName, sqlFile, query)

          Files.write(
            resultsPath,
            (queryName + extractedSeq + "\n").getBytes,
            StandardOpenOption.APPEND
          )

        )

      }
      conn.close()
    }
  }
}
