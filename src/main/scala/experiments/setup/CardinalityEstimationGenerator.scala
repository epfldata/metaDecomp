package experiments.setup

import decompositions.*
import experiments.Config.{benchmarks, dataSource, sqlFilesInBenchmark}
import experiments.readInOneLine
import experiments.runner.BaseRunner
import decompositions.Hypergraph.{Vertex, Hyperedge}
import sql.SQLParser
import scala.concurrent.ExecutionContext.Implicits.global

import java.nio.file.{Files, Paths}
import java.sql.DriverManager
import scala.collection.mutable
import scala.io.Source
import experiments.Config.benchmarksPath
import java.nio.file.StandardOpenOption
import scala.collection.immutable.BitSet
import scala.sys.process._
import experiments.Config.dbFilePath
import scala.concurrent.duration.*
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.TimeoutException
import java.sql.SQLTimeoutException

object CardinalityEstimationGenerator extends BaseRunner {
  def main(args: Array[String]): Unit = {
    for (benchmark <- if args.size >= 1 then List(args(0)) else benchmarks ;
         sqlFile <- sqlFilesInBenchmark(benchmark).filter(f => if args.size >= 2 then f.getName.matches(args(1)) else true) ) {
      connect(benchmark)

      val queryName = sqlFile.getName.stripSuffix(".sql")

      val query = readInOneLine(sqlFile)


      val sqlIR = SQLParser.parse(query)

      val benchmarkCardinalitiesPath = Paths.get(s"${benchmarksPath}/${benchmark}/cardinalities")

      val resultsFile = Paths.get(s"${benchmarkCardinalitiesPath}", s"${queryName}.csv")

      if (!Files.exists(resultsFile)) {
        println(s"${sqlFile.getName}")
        Files.createFile(resultsFile)

        val queryGraphEdges =
          for e1 <- sqlIR.hyperedges; e2 <- sqlIR.hyperedges if e1 != e2 && e1.nodes.intersect(e2.nodes).nonEmpty
            yield Set(e1, e2)

        val orderedHyperedges = sqlIR.hyperedges.toSeq
        val hyperedgeToIndex = orderedHyperedges.zipWithIndex.toMap
        val numHyperedges = orderedHyperedges.size

        val adjIndices: Array[Set[Int]] = Array.tabulate(numHyperedges) { i =>
          val e1 = orderedHyperedges(i)
          orderedHyperedges.zipWithIndex.collect {
            case (e2, j) if i != j && e1.nodes.intersect(e2.nodes).nonEmpty => j
          }.toSet
        }

        def getNonEmptySubsets(set: Set[Int]): Array[Set[Int]] = {
          val arr = set.toArray
          val k = arr.length
          val numSubsets = (1 << k) - 1
          val result = new Array[Set[Int]](numSubsets)
          var mask = 1
          while (mask <= numSubsets) {
            val builder = Set.newBuilder[Int]
            var idx = 0
            var m = mask
            while (m > 0) {
              if ((m & 1) != 0) builder += arr(idx)
              m >>= 1
              idx += 1
            }
            result(mask - 1) = builder.result()
            mask += 1
          }
          result
        }

        val orderedBitSets = mutable.ListBuffer[Long]()
        val cardinalities = mutable.ListBuffer[Long]()

        def processCsg(csgIndices: Set[Int]): Unit = {
          val csg = csgIndices.map(orderedHyperedges)

          val bitSetAsLong = csgIndices.foldLeft(0L)((mask, idx) => mask | (1L << idx))
          orderedBitSets.append(bitSetAsLong)

          val joinConditions = (for t1 <- csg; t2 <- csg if t1 != t2 yield Set(t1, t2)).flatMap(pair => {
            val t1 = pair.head
            val t2 = pair.tail.head
            t1.nodes.intersect(t2.nodes)
              .map(n => f"${t1.alias}.${sqlIR.vertexIdToColumnName(n.name)(t1)} = ${t2.alias}.${sqlIR.vertexIdToColumnName(n.name)(t2)}")
          })
          val filterConditions = sqlIR.filterConditions.keys.filter(_.subsetOf(csg)).flatMap(sqlIR.filterConditions)
          val projectTo =
            sqlIR.outputAttributes.filter(outputColumn => csg.contains(outputColumn.qualifiedCol.hyperedge)).map(_.withoutAggregation)
              ++ csg.flatMap(_.nodes).intersect((sqlIR.hyperedges -- csg).flatMap(_.nodes)).map(node => {
              val hyperedge = csg.find(_.nodes.contains(node)).get
              val colName = sqlIR.vertexIdToColumnName(node.name)(hyperedge)
              OutputItem(QualifiedCol(hyperedge, colName))
            })
              ++ filterConditions.flatMap(_.referencedCols).map(OutputItem(_, None)) // TODO: Is this correct?

          val projectToAsString = projectTo.map(outputColumn => s"${outputColumn.qualifiedCol.hyperedge.alias}.${outputColumn.qualifiedCol.column}").mkString(", ")

          val query = s"EXPLAIN (FORMAT JSON) SELECT COUNT(*) FROM ( SELECT "
            + projectToAsString
          + s" FROM ${csg.map(n => s"${n.tableName} AS ${n.alias}").mkString(", ")} "
            + (if joinConditions.nonEmpty || filterConditions.nonEmpty then f" WHERE ${(joinConditions ++ filterConditions.map(_.conditionText)).mkString(" AND ")}" else "")
            + s" GROUP BY ${projectToAsString}"
            + ");"

          val timeout = 30.seconds
          val stmt = conn.createStatement()
          stmt.setQueryTimeout(timeout.toSeconds.toInt)

          val rs = stmt.executeQuery(query)
          if (rs.next) {
            val jsonStr = rs.getString(2)
            val pattern = """\"Estimated Cardinality\": \"(\d+)\"""".r
            val matches = pattern.findAllIn(jsonStr).matchData.toSeq
            if (matches.nonEmpty) {
              val lastMatch = matches.last
              val cardinalityString = lastMatch.group(1)
              val cardinality = BigInt(cardinalityString)
              val cappedCardinality = if (cardinality > (1L << 62) - 1) (1L << 62) - 1 else cardinality.toLong
              cardinalities.append(cappedCardinality)
            } else {
              cardinalities.append((1L << 62) - 1)
            }
          }

          if (cardinalities.size % 10000 == 0) {
            println(s"Processed ${cardinalities.size} CSGs")
          }

          stmt.close()
        }

        def enumerateCSGRec(s: Set[Int], x: Set[Int]): Unit = {
          val n = s.flatMap(adjIndices).diff(s).diff(x)
          if (n.nonEmpty) {
            val nSubsets = getNonEmptySubsets(n)
            for (nPrime <- nSubsets) {
              val sPrime = s ++ nPrime
              processCsg(sPrime)
            }
            val newX = x ++ n
            for (nPrime <- nSubsets) {
              val sPrime = s ++ nPrime
              enumerateCSGRec(sPrime, newX)
            }
          }
        }

        for (i <- (numHyperedges - 1) to 0 by -1) {
          val s = Set(i)
          val x = (0 to i).toSet
          processCsg(s)
          enumerateCSGRec(s, x)
        }

        Files.write(resultsFile, s"${sqlIR.hyperedges.size} ${queryGraphEdges.size} ${orderedBitSets.size}\n${orderedHyperedges.map(_.alias).mkString(" ")}\n${queryGraphEdges.map(_.map(hyperedgeToIndex).mkString(" ")).mkString(" ")}\n${(orderedBitSets.zip(cardinalities).map { case (bitSet, card) => s"$bitSet $card" }).mkString("\n")}\n".getBytes, StandardOpenOption.APPEND)
      }

      conn.close()
    }
  }
}
