package experiments.runner
import decompositions.{Hypergraph, HypergraphIO, KDecomp, MetaDecompBasedOptimizer, MetaDecompCyclicOptimizer, MetaDecompGraph, MetaDecompGraphConstructBaseline, MetaDecompGraphConstructInterpolatable, MetaDecompGraphConstructor, RandomHypergraph, metaGYO}
import experiments.Config.{benchmarks, benchmarksPath, repeatTimes, resultsDir, sqlFilesInBenchmark}

import scala.io.Source
import sql.{IR, SQLParser}

import java.nio.file.{Files, Paths, StandardOpenOption}
import utils.{DBInspector, QueryToSql, RandomDbGenerator, Schema, SchemaReader}

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

def readAndCheckSchema(dirPath: String) : Schema = {
  val schemaPath = dirPath + "/schema.txt"
  val schema: Schema = SchemaReader.readSchema(schemaPath)
  schema.tables.foreach(table => println(table))

  var ok: Boolean = true
  Files.list(Paths.get(dirPath)).forEach(path => {
    println(s"checking $path")
    SchemaReader.readSchema(path.toString).tables.foreach(table => {
      if (!schema.tables.contains(table)) {
        println(s"${path} failed: $table not found in schema")
        ok = false
      }
    })
  })
  if (ok) println("All succeeded")
  schema
}

def constructDB(benchmarkName: String, intRange: Int = 20, rowCount: Int = 1000): Unit = {
  val schema = readAndCheckSchema("./benchmarks/" + benchmarkName + "-hypergraphs/")

  val fullBenchmarkName = benchmarkName + s"-range$intRange-rowcnt$rowCount"
  val dbPath = s"./datasets/$fullBenchmarkName/$fullBenchmarkName.db"
  RandomDbGenerator.generate(schema, dbPath, rowCount, intRange, 0.0)
}

def convertQueriesToSQL(benchmarkName: String, arity: Int): Unit = {
  val hypergraphsPath = "./benchmarks/" + benchmarkName + "-hypergraphs"
  Files.list(Paths.get(hypergraphsPath)).forEach(path => {
    if (!path.getFileName.toString.endsWith("predecessor.txt") && !path.getFileName.toString.endsWith("sql")) {
      println(s"Converting $path")
      QueryToSql.convert(path, arity)
    }
  })
}

def parseWidthAndNumRel(path: String): (Int, Int) = {
  val pattern = """.*/q(\d+)-w(\d+)-r(\d+)\.sql$""".r
  path match {
    case pattern(_, width, numRel) => (width.toInt, numRel.toInt)
    case _ => throw new IllegalArgumentException(s"Path does not match expected format: $path")
  }
}

def validate(benchmarkName: String): Unit = {
  val queriesPath = "./benchmarks/" + benchmarkName + "/queries"
  var cnt = 0
  var allChecksPassed = true
  Files.list(Paths.get(queriesPath)).forEach(sqlFile => {
    val expectedPair = parseWidthAndNumRel(sqlFile.toString)
    val expectedWidth = expectedPair._1
    val expectedNumRel = expectedPair._2

    cnt += 1
    println(s"$sqlFile: ")
    val source = Source.fromFile(sqlFile.toString)
    val query = source.getLines().mkString(" ")
    source.close()

    implicit val sqlIR: sql.IR = SQLParser.parse(query)
    val hypergraph = Hypergraph(sqlIR.hyperedges.flatMap(_.nodes).toSet, sqlIR.hyperedges)
    val width = getWidth(hypergraph)
    println(s"width: $width, expected width: $expectedWidth, equal: ${width == expectedWidth}")
    val numRel = hypergraph.edges.size
    println(s"num rels: $numRel, expected num rels: $expectedNumRel, equal: ${numRel == expectedNumRel}")
    if ((width != expectedWidth) || (numRel != expectedNumRel)) allChecksPassed = false
    //println(hypergraph)


  })
  println(s"$cnt queries total")
  println(s"All checks passed: $allChecksPassed")
}


object BenchmarkGenerator{
    def main(args: Array[String]): Unit = {
      val benchmarkName = "custom-ar3-new"
      val intRange = 2000
      val rowCount = 20000
      //convertQueriesToSQL(benchmarkName, 6)

      // ------------ validation ---------------
      //validate(benchmarkName)


      // -------------- DB construction -------------
      constructDB(benchmarkName, intRange, rowCount)
      val fullBenchmarkName = benchmarkName + s"-range$intRange-rowcnt$rowCount"
      val dbPath = s"./datasets/$fullBenchmarkName/$fullBenchmarkName.db"
      DBInspector.inspect(dbPath)


    }
}
