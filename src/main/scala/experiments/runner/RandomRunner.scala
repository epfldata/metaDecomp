package experiments.runner
import decompositions.{Hypergraph, RandomHypergraph, MetaDecompBasedOptimizer, MetaDecompCyclicOptimizer, MetaDecompGraph, MetaDecompGraphConstructBaseline, MetaDecompGraphConstructInterpolatable, MetaDecompGraphConstructor, metaGYO, HypergraphIO, KDecomp}
import experiments.Config.{benchmarks, benchmarksPath, repeatTimes, resultsDir, sqlFilesInBenchmark}
import sql.{IR, SQLParser}

import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.sys.process.*
import experiments.parseSubqueryTables
import experiments.getTimestamp


object RandomRunner {
  def hypergraphFromEdges(edgesByIdVertices: List[(String, Set[String | Int])]): Hypergraph = {
    val vertexMap = edgesByIdVertices.flatMap(_._2).map(id => id -> Hypergraph.Vertex(id.toString)).toMap
    val edges = edgesByIdVertices.map { case (id, vertexIds) => Hypergraph.Hyperedge(vertexIds.map(vertexMap(_)), id) }.toSet
    Hypergraph(vertexMap.values.toSet, edges)
  }

  def getWidthBaseline(hypergraph: Hypergraph): Int = {
    var width = 1
    var meta: MetaDecompGraph = null
    while ( {
      meta = MetaDecompGraphConstructBaseline().run(hypergraph, width);
      meta.sortedEdges.isEmpty
    }) {
      println(s"Width ${width} failed")
      width += 1
    }
    println(s"Width ${width}")
    width
  }

  def getWidth(hypergraph: Hypergraph): Int = {
    var width = 1

    while ( {
      !KDecomp().run(hypergraph, width)
    }) {
      println(s"Width ${width} failed")
      width += 1
    }
    println(s"Width ${width}")
    width
  }

  def printHypergraph(hypergraph: Hypergraph): Unit = {
    println(hypergraph)
    println(s"${hypergraph.vertices.size} vertices, ${hypergraph.edges.size} edges")
  }

  def main(args: Array[String]): Unit = {
    val n = args(0).toInt
    val m = args(1).toInt
    val c = args(2).toInt
    val batchSize = if (args.length > 3) {
      args(3).toInt
    } else {
      10
    }
    val reps = if (args.length > 4) {
      args(4).toInt
    } else {
      1
    }
    val step = if (args.length > 5) {
      args(5).toInt
    } else {
      0
    }


    var initialHypergraph = HypergraphIO.readFromFile("./benchmarks/custom-ar3-new-hypergraphs/schema.txt")
    var initialWidth = getWidth(initialHypergraph)
    print(s"hw: $initialWidth")
    /*val hypergraph = hypergraphFromEdges(List(
							("e1", Set("1", "12", "4")),
							("e2", Set("4", "5", "6")),
							("e3", Set("1", "2", "9")),
							("e4", Set("7", "8", "9")),
							("e5", Set("10", "3", "4")),
							("e6", Set("12", "11", "4")),
							//("e7", Set("7", "8")),
							//("e8", Set("4", "7")),
							//("e9", Set("5", "6"))
      ))*/


    printHypergraph(initialHypergraph)
    val timestamp = getTimestamp
    val dir = s"./benchmarks/custom-ar3-extra"
    val dirPath = Paths.get(dir)
    Files.createDirectories(dirPath)
    val totalQueries = batchSize * reps

    //val initialPath = dir + s"/schema.txt"
    //HypergraphIO.writeToFile(initialHypergraph, initialPath)

    for (i <- 0 until totalQueries) {
      println(s"---------------- run $i -----------------")
      if ((i != 0) && (i % batchSize == 0)) {
        for (_ <- 0 until step) {
          initialHypergraph.removeRandomEdgeKeepingConnected()
        }
        initialWidth = getWidth(initialHypergraph)
      }
      val hypergraph = Hypergraph(initialHypergraph.vertices, initialHypergraph.edges)
      var prevWidth = initialWidth

      var widthDecreased: Boolean = false
      var stop = false
      while (prevWidth != 1 && !widthDecreased && !stop) {
        hypergraph.removeRandomEdgeKeepingConnected() match {
          case Some(hyperedge) => {
            printHypergraph(hypergraph)
            

            val currWidth = getWidth(hypergraph)
            if (currWidth < prevWidth) {
              widthDecreased = true
              println("Width decreased")

              val lowerPath = dir + s"/q$i-w$currWidth-r${hypergraph.edges.size}-c.txt"
              HypergraphIO.writeToFile(hypergraph, lowerPath)

              hypergraph.addEdge(hyperedge)
              val higherPath = dir + s"/q$i-w$currWidth-r${hypergraph.edges.size-1}-c-predecessor.txt"
              HypergraphIO.writeToFile(hypergraph, higherPath)

              // --------------------- validation -------------------------
              val lowerWidthHg = HypergraphIO.readFromFile(lowerPath)
              val higherWidthHg = HypergraphIO.readFromFile(higherPath)
              val lowerWidth = getWidth(lowerWidthHg)
              val higherWidth = getWidth(higherWidthHg)
              println(s"run $i: width of final query: $lowerWidth, previous width: $higherWidth")
            } else {
              prevWidth = currWidth
            }
          }
          case None => {
            stop = true
            println("Can't delete an edge without breaking connectedness")
          }
        }
      }



    }

  /*val lowerWidthHg = HypergraphIO.readFromFile("./benchmarks/custom-ar3-hypergraphs/q0-w3-r24.txt")
  val higherWidthHg = HypergraphIO.readFromFile("./benchmarks/custom-ar3-hypergraphs/q0-w3-r24-unique.txt")
  val lowerWidth = getWidth(lowerWidthHg)
  val higherWidth = getWidth(higherWidthHg)
  println(s"$lowerWidth vs. $higherWidth")*/


  }
}
