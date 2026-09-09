package experiments.runner

import decompositions.{Hypergraph, MetaDecompBasedOptimizer, MetaDecompCyclicOptimizer, MetaDecompGraph, MetaDecompGraphConstructBaseline, MetaDecompGraphConstructInterpolatable, MetaDecompGraphConstructor, metaGYO}
import experiments.Config.{benchmarks, benchmarksPath, repeatTimes, resultsDir, sqlFilesInBenchmark}
import sql.{IR, SQLParser}

import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.sys.process.*
import experiments.parseSubqueryTables
import experiments.getTimestamp

object MetaDecompInterpolatableConstructRunner extends BaseRunner {
  def main(args: Array[String]): Unit = {
    for (benchmark <- if args.size >= 1 then List(args(0)) else benchmarks) {
      connect(benchmark)

      val benchmarkPath = s"$benchmarksPath/$benchmark"
      val resultsPath = Paths.get(resultsDir, s"metadecomp-construct-comparison-$benchmark-$getTimestamp.csv")
      Files.write(
        resultsPath,
        "query,meta_time,meta_vertices,meta_edges,ip_time,ip_vertices,ip_edges,are_equal\n".getBytes,
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

            //println(hypergraph)

            var width = 2
            var meta: MetaDecompGraph = null
            while ({ meta = MetaDecompGraphConstructBaseline().run(hypergraph, width); meta.sortedEdges.isEmpty } ) {
              println(s"Width ${width} failed")
              width += 1
            }
            println(s"Width ${width}")

            val metaInterpolatable = MetaDecompGraphConstructInterpolatable().run(hypergraph, width)

            val meta_edges = meta.sortedEdges.size
            val ip_edges = metaInterpolatable.sortedEdges.size
            val meta_vertices = meta.getVertices.size
            val ip_vertices = metaInterpolatable.getVertices.size

            val isSubset = metaInterpolatable.sortedEdges.toSet.subsetOf(meta.sortedEdges.toSet)
            println(s"Interpolatable metaDecomp is a subgraph of regular metaDecomp: $isSubset")

            println(s"IP vertices: $ip_vertices, meta vertices: $meta_vertices")
            println(s"IP edges: $ip_edges, meta edges: $meta_edges")


            val metaGraphTime = (for (i <- 0 until repeatTimes) yield {
              val metaStartTime = System.nanoTime()
              MetaDecompGraphConstructor().run(hypergraph, width)
              val metaEndTime = System.nanoTime()
              val metaTime = (metaEndTime - metaStartTime) / 1000 // microseconds
              println(s"Meta graph construction run $i: $metaTime us")


              metaTime
            }).sorted.apply(repeatTimes / 2) // returns the median

            val metaGraphTimeInterpolatable = (for (i <- 0 until repeatTimes) yield {
              val metaStartTimeInterpolatable = System.nanoTime()
              MetaDecompGraphConstructInterpolatable().run(hypergraph, width)
              val metaEndTimeInterpolatable = System.nanoTime()
              val metaTimeInterpolatable = (metaEndTimeInterpolatable - metaStartTimeInterpolatable) / 1000 // microseconds
              println(s"Meta graph construction run $i: $metaTimeInterpolatable us (baseline)")

              metaTimeInterpolatable
            }).sorted.apply(repeatTimes / 2) // returns the median

            // "query,meta_time,meta_vertices,meta_edges,ip_time,ip_vertices,ip_edges,are_equal\n"
            println(s"${sqlFile.getName},$metaGraphTime,$metaGraphTimeInterpolatable,$isSubset")
            Files.write(
              resultsPath,
              s"${sqlFile.getName},$metaGraphTime,$meta_vertices,$meta_edges,$metaGraphTimeInterpolatable,$ip_vertices,$ip_edges,$isSubset\n".getBytes,
              StandardOpenOption.APPEND
            )

          }
        }
      )
      conn.close()
    }
  }
}

