package experiments.runner

import experiments.Config.*
import experiments.getTimestamp
import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.jdk.CollectionConverters.*
import scala.util.Using

object LLMR2Runner extends BaseRunner {
  def main(args: Array[String]): Unit = {
    for (benchmark <- benchmarks) {
      connect(benchmark)

      val timestamp = getTimestamp
      val resultsCsvPath = Paths.get(resultsDir, s"llm-r2-opt-$benchmark-$timestamp.csv")

      Files.write(
        resultsCsvPath,
        "query,opt_time,exec_time,total_time\n".getBytes,
        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
      )

      println(s"\nProcessing benchmark: $benchmark")

      // 1. Read Query IDs
      // Path: llm-r2/data/queries/queries_[benchmark]_metadecomp.csv
      // Column 'db_id' is index 1 (cols 0,1,2...). Actually index 0 is rownum maybe?
      // Header: ,db_id,original_sql
      // Wait, user said "query IDs ... look them up in ... queries_[benchmark]_metadecomp.csv".
      // From previous `head` output: `query013_spj_0,dsb-metadecomp,"select ..."` 
      // It seems the first column (index 0) is the ID (e.g., query013_spj_0).
      val queriesCsvPath = s"llm-r2/data/queries/queries_${benchmark}_metadecomp.csv"
      val queryIds = Using(scala.io.Source.fromFile(queriesCsvPath)) { source =>
        source.getLines().drop(1).map { line =>
          // CSV parsing verify simple split. Quoted SQL might contain commas.
          // Since we only need the first column (ID), splitting by comma is risky if ID has comma (unlikely).
          // But SQL is last. The ID is first.
          line.split(",")(0) 
        }.toVector
      }.get

      // 2. Read Rewritten SQL
      // Path: llm-r2/gpt_{benchmark}_one_promo_queryCL_filt_schema_none.csv
      // format unknown for sure, need to be careful about CSV parsing with quotes.
      // We'll use a regex or a simple CSV parser if needed. 
      // The file has a header. 'rewritten_sql_gpt' is the target.
      val gptCsvPath = s"llm-r2/gpt_${benchmark}_one_promo_queryCL_filt_schema_none.csv"
      // We need robust CSV parsing because SQL contains commas and quotes.
      // Let's assume standard CSV format with headers.
      // Since we don't have a library, we'll try a regex for splitting CSV lines respecting quotes.
      val rewrittenSqls = readColumnFromCsv(gptCsvPath, "rewritten_sql_gpt")

      // 3. Read Optimization Times
      // Path: llm-r2/time_gpt_{benchmark}_one_promo_queryCL_filt_schema_none.csv
      // Columns: ,demo_time,llm_time,rewriter_time
      val timeCsvPath = s"llm-r2/time_gpt_${benchmark}_one_promo_queryCL_filt_schema_none.csv"
      val optTimes = Using(scala.io.Source.fromFile(timeCsvPath)) { source =>
        val lines = source.getLines().toVector;
        val header = lines.head.split(",")
        val demoIdx = header.indexOf("demo_time")
        val llmIdx = header.indexOf("llm_time")
        val rwIdx = header.indexOf("rewriter_time")
        
        lines.drop(1).map { line =>
           val cols = line.split(",") // These are numbers, safe to split
           val t1 = cols(demoIdx).toDouble
           val t2 = cols(llmIdx).toDouble
           val t3 = cols(rwIdx).toDouble
           // Sum and convert seconds to microseconds
           ((t1 + t2 + t3) * 1_000_000).toLong
        }
      }.get

      // Verify lengths match
      if (queryIds.size != rewrittenSqls.size || queryIds.size != optTimes.size) {
        println(s"WARNING: Mismatch in row counts for $benchmark!")
        println(s"IDs: ${queryIds.size}, SQLs: ${rewrittenSqls.size}, Times: ${optTimes.size}")
        // We proceed with the minimum length to avoid crash
      }

      val limit = Seq(queryIds.size, rewrittenSqls.size, optTimes.size).min

      for (i <- 0 until limit) {
        val qid = queryIds(i)
        val sql = rewrittenSqls(i)
        val optTimeUs = optTimes(i)


        if (!qid.matches("""([0-9][0-9][a-z][a-z]|0|1[0-4]|15[0-3]).*""")) {
          println(s"Processing query $qid")

          // Clean up SQL if necessary (remove quotes if CSV parser kept them?) 
          // My readColumnFromCsv should handle content extraction.
          // Also handle "NA" or empty queries
          if (sql.trim.toUpperCase == "NA" || sql.trim.isEmpty) {
            // Skip or log error? User didn't specify. Assuming skip execution, maybe log time?
            // If SQL is invalid, can't run.
            Files.write(resultsCsvPath, s"$qid,$optTimeUs,-1,-1\n".getBytes, StandardOpenOption.APPEND)
          } else {
            try {

              // Sanitize SQL: replace all whitespace-like chars with standard space
              var cleanSql = sql.replaceAll("[\\s\\u00A0]+", " ").trim
              
              // Fix PostgreSQL SUBSTR syntax: SUBSTR(str FROM start FOR length) -> SUBSTR(str, start, length)
              // Case insensitive replacement for SUBSTR, FROM, FOR
              cleanSql = cleanSql.replaceAll("(?i)SUBSTR\\s*\\((.+?)\\s+FROM\\s+(.+?)\\s+FOR\\s+(.+?)\\)", "SUBSTR($1, $2, $3)")
              
              // Fix rewriter corruption where identifiers were split by quotes or over-quoted
              // Matches any sequence containing at least one quote and alphanumerics/underscores.
              // e.g. ""label""_alias -> "label_alias", ""g""id"""" -> "gid", "word"_word -> "word_word"
              val dirtyIdRegex = "([a-zA-Z0-9_]*\"[a-zA-Z0-9_\"]*)".r
              cleanSql = dirtyIdRegex.replaceAllIn(cleanSql, m => "\"" + m.group(1).replace("\"", "") + "\"")
              // cleanSql += ";" // BaseRunner adds a semicolon
              
              // Iteratively run EXPLAIN to catch and fix multiple Binder Errors
              var isValid = false
              var retries = 0
              val maxRetries = 20
              
              while (!isValid && retries < maxRetries) {
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
            } catch {
              case e: Exception =>
                println(s"Error executing $qid: ${e.getMessage}")
                Files.write(resultsCsvPath, s"$qid,$optTimeUs,-1,-1\n".getBytes, StandardOpenOption.APPEND)
            }
          }
        }
      }

      conn.close()
    }
  }

  // Rudimentary CSV parser to handle specific column extraction
  def readColumnFromCsv(path: String, colName: String): Vector[String] = {
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
