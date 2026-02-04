package experiments.runner

import experiments.Config.*
import experiments.getTimestamp
import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.jdk.CollectionConverters.*
import scala.util.Using

object LearnedRewriteRunner extends BaseRunner {
  def main(args: Array[String]): Unit = {
    for (benchmark <- benchmarks) {
      connect(benchmark)

      val timestamp = getTimestamp
      val resultsCsvPath = Paths.get(resultsDir, s"learned-rewrite-opt-$benchmark-$timestamp.csv")

      Files.write(
        resultsCsvPath,
        "query,opt_time,exec_time,total_time\n".getBytes,
        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
      )

      println(s"\nProcessing benchmark: $benchmark")

      // 1. Read Query IDs
      val queriesCsvPath = s"llm-r2/data/queries/queries_${benchmark}_metadecomp.csv"
      val queryIds = Using(scala.io.Source.fromFile(queriesCsvPath)) { source =>
        source.getLines().drop(1).map { line =>
          line.split(",")(0) 
        }.toVector
      }.get

      // 2. Read Rewritten SQL and Rewrite Time
      // Path: llm-r2/LR_{benchmark}_results.csv
      val lrCsvPath = s"llm-r2/LR_${benchmark}_results.csv"
      
      // We need to read both rewritten_sql and rewrite_time
      // Since readColumnFromCsv only reads one column, we can either call it twice or modify it.
      // Calling it twice is easier for now, though slightly inefficient (parsing file twice).
      // Given file/dataset size, it's acceptable.
      val rewrittenSqls = readColumnFromCsv(lrCsvPath, "rewritten_sql")
      val rewriteTimesStr = readColumnFromCsv(lrCsvPath, "rewrite_time")
      
      val rewriteTimesUs = rewriteTimesStr.map { t =>
        if (t.isEmpty || t == "NA") 0L
        else (t.toDouble * 1_000_000).toLong
      }

      // Verify lengths match
      if (queryIds.size != rewrittenSqls.size || queryIds.size != rewriteTimesUs.size) {
        println(s"WARNING: Mismatch in row counts for $benchmark!")
        println(s"IDs: ${queryIds.size}, SQLs: ${rewrittenSqls.size}, Times: ${rewriteTimesUs.size}")
      }

      val limit = Seq(queryIds.size, rewrittenSqls.size, rewriteTimesUs.size).min

      for (i <- 0 until limit) {
        val qid = queryIds(i)
        val sql = rewrittenSqls(i)
        val optTimeUs = rewriteTimesUs(i)

        println(s"Processing query $qid")

        if (sql.trim.toUpperCase == "NA" || sql.trim.isEmpty) {
          Files.write(resultsCsvPath, s"$qid,$optTimeUs,-1,-1\n".getBytes, StandardOpenOption.APPEND)
        } else {
          try {
            // Sanitize SQL: replace all whitespace-like chars with standard space
            var cleanSql = sql.replaceAll("[\\s\\u00A0]+", " ").trim
            
            // Fix PostgreSQL SUBSTR syntax: SUBSTR(str FROM start FOR length) -> SUBSTR(str, start, length)
            cleanSql = cleanSql.replaceAll("(?i)SUBSTR\\s*\\((.+?)\\s+FROM\\s+(.+?)\\s+FOR\\s+(.+?)\\)", "SUBSTR($1, $2, $3)")
            
            // Fix rewriter corruption where identifiers were split by quotes or over-quoted
            // e.g. ""label""_alias -> "label_alias", ""g""id"""" -> "gid", "word"_word -> "word_word"
            val dirtyIdRegex = "([a-zA-Z0-9_]*\"[a-zA-Z0-9_\"]*)".r
            cleanSql = dirtyIdRegex.replaceAllIn(cleanSql, m => "\"" + m.group(1).replace("\"", "") + "\"")
            
            // cleanSql += ";" // BaseRunner adds a semicolon
            
            // Iteratively run EXPLAIN to catch and fix multiple Binder Errors
            var isValid = false
            var retries = 0
            val maxRetries = 20
            
            while (!isValid) {
                try {
                  val stmt = conn.createStatement()
                  stmt.execute("EXPLAIN " + cleanSql)
                  stmt.close()
                  isValid = true // Passed validation
                } catch {
                    case e: java.sql.SQLException if e.getMessage.contains("Binder Error") && e.getMessage.contains("does not have a column named") =>
                      retries += 1
                      // Extract column name
                      val pattern = "does not have a column named \"(.*)\"".r
                      val matchOpt = pattern.findFirstMatchIn(e.getMessage)
                      if (matchOpt.isDefined) {
                          val badCol = matchOpt.get.group(1)
                          // Fix: Handle any trailing digits, not just "0" (e.g. name2, id05)
                          if (badCol.matches(".*\\d+$")) {
                              val goodCol = badCol.replaceAll("\\d+$", "")
                              Console.err.println(s"INFO: Repaired SQL column $badCol -> $goodCol (Attempt $retries)")
                              // Use regex to replace whole word to avoid partial matches
                              cleanSql = cleanSql.replaceAll("\\b" + java.util.regex.Pattern.quote(badCol) + "\\b", goodCol)
                              // Loop continues to verify the fix and find next error
                          } else {
                              Console.err.println(s"WARNING: Binder error column $badCol does not end in digits, skipping repair.")
                              isValid = true // Cannot fix, exit loop and let runQuery fail
                          }
                      } else {
                          Console.err.println("WARNING: Failed to extract column name from Binder Error.")
                          isValid = true
                      }
                    case _: Throwable => 
                      isValid = true // Other errors: stop checking, let runQuery handle/fail
                }
            }
            
            if (retries >= maxRetries) {
                Console.err.println(s"WARNING: Max retries ($maxRetries) reached for query validation.")
            }
            
            val execTimeUs = runQuery(cleanSql)
            val totalTimeUs = optTimeUs + execTimeUs
            Files.write(resultsCsvPath, s"$qid,$optTimeUs,$execTimeUs,$totalTimeUs\n".getBytes, StandardOpenOption.APPEND)
            println(s"Executed $benchmark/$qid: Total $totalTimeUs us")
          } catch {
            case e: Exception =>
              println(s"Error executing $qid: ${e.getMessage}")
              Files.write(resultsCsvPath, s"$qid,$optTimeUs,-1,-1\n".getBytes, StandardOpenOption.APPEND)
          }
        }
      }

      conn.close()
    }
  }

  // Rudimentary CSV parser to handle specific column extraction
  def readColumnFromCsv(path: String, colName: String): Vector[String] = {
    // Check if file exists first
    if (!Files.exists(Paths.get(path))) {
        println(s"WARNING: File not found: $path")
        return Vector.empty
    }

    Using(scala.io.Source.fromFile(path)) { source =>
      val lines = source.getLines().toVector
      if (lines.isEmpty) return Vector.empty

      val headerLine = lines.head
      // Naive split for header
      val headers = headerLine.split(",").map(_.trim.replace("\"", "")) 
      val colIdx = headers.indexOf(colName)
      
      if (colIdx == -1) throw new RuntimeException(s"Column $colName not found in $path")

      lines.drop(1).map { line =>
        // Regex to split by comma, ignoring commas inside quotes
        val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1)
        if (colIdx < parts.length) {
          // Remove wrapping quotes if present and unescape double quotes
          var content = parts(colIdx).trim
          if (content.startsWith("\"") && content.endsWith("\"")) {
            content = content.drop(1).dropRight(1).replace("\"\"", "\"")
          }
          content
        } else {
          ""
        }
      }
    }.get
  }
}
