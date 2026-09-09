package utils

import java.sql.{Connection, DriverManager}
import scala.util.Random
import scala.collection.mutable.ListBuffer

object DBInspector {
  case class ColumnInfo(name: String, sqlType: String)
  case class TableInfo(name: String, columns: List[ColumnInfo])

  private def connect(dbPath: String): Connection = {
    Class.forName("org.duckdb.DuckDBDriver")
    DriverManager.getConnection(s"jdbc:duckdb:$dbPath")
  }

  /** Reads all tables and columns in the main schema of a DuckDB database file. */
  def readSchema(dbPath: String): List[TableInfo] = {
    val conn = connect(dbPath)
    try {
      val tableNames = ListBuffer.empty[String]

      val tablesStatement = conn.prepareStatement(
        """SELECT table_name
          |FROM information_schema.tables
          |WHERE table_schema = 'main' AND table_type = 'BASE TABLE'
          |ORDER BY table_name""".stripMargin
      )
      try {
        val tablesRs = tablesStatement.executeQuery()
        try {
          while (tablesRs.next()) tableNames += tablesRs.getString("table_name")
        } finally tablesRs.close()
      } finally tablesStatement.close()

      tableNames.toList.map { tableName =>
        val columns = ListBuffer.empty[ColumnInfo]
        val columnsStatement = conn.prepareStatement(
          """SELECT column_name, data_type
            |FROM information_schema.columns
            |WHERE table_schema = 'main' AND table_name = ?
            |ORDER BY ordinal_position""".stripMargin
        )
        try {
          columnsStatement.setString(1, tableName)
          val columnsRs = columnsStatement.executeQuery()
          try {
            while (columnsRs.next()) {
              columns += ColumnInfo(
                columnsRs.getString("column_name"),
                columnsRs.getString("data_type")
              )
            }
          } finally columnsRs.close()
        } finally columnsStatement.close()
        TableInfo(tableName, columns.toList)
      }
    } finally conn.close()
  }

  /** Returns the total row count for a table. */
  private def rowCount(conn: Connection, tableName: String): Long = {
    val stmt = conn.createStatement()
    try {
      val rs = stmt.executeQuery(s"SELECT COUNT(*) AS cnt FROM $tableName")
      try {
        rs.next()
        rs.getLong("cnt")
      } finally rs.close()
    } finally stmt.close()
  }

  /** Samples up to `n` random rows from a table, returned as a list of column-name -> value maps. */
  def sampleRandomRows(conn: Connection, table: TableInfo, n: Int, rng: Random): List[Map[String, Any]] = {
    val total = rowCount(conn, table.name)
    if (total == 0) return List.empty

    // Pick n random OFFSETs (with replacement, simple approach; fine for exploratory printing)
    val offsets = (1 to math.min(n, total.toInt)).map(_ => rng.nextInt(total.toInt)).distinct

    offsets.flatMap { offset =>
      val stmt = conn.createStatement()
      try {
        val rs = stmt.executeQuery(s"SELECT * FROM ${table.name} LIMIT 1 OFFSET $offset")
        try {
          if (rs.next()) {
            Some(table.columns.map(c => c.name -> rs.getObject(c.name)).toMap)
          } else None
        } finally rs.close()
      } finally stmt.close()
    }.toList
  }

  /** Full inspection: prints table count, schema, and 2 random sample rows per table. */
  def inspect(dbPath: String, samplesPerTable: Int = 2, seed: Option[Long] = None): Unit = {
    val rng = seed.map(new Random(_)).getOrElse(new Random())
    val schema = readSchema(dbPath)

    println(s"Total tables: ${schema.size}")
    println("Schema:")
    schema.foreach { table =>
      println(s"  ${table.name}(${table.columns.map(c => s"${c.name}: ${c.sqlType}").mkString(", ")})")
    }
    println()

    val conn = connect(dbPath)
    try {
      schema.foreach { table =>
        println(s"Table: ${table.name}")
        println(s"  Columns: ${table.columns.map(_.name).mkString(", ")}")
        println(s"Rows: ${rowCount(conn, table.name)}")

        val samples = sampleRandomRows(conn, table, samplesPerTable, rng)
        if (samples.isEmpty) {
          println("  (no rows)")
        } else {
          samples.foreach { row =>
            val rowStr = table.columns.map(c => s"${c.name}=${row.getOrElse(c.name, "NULL")}").mkString(", ")
            println(s"  Row: $rowStr")
          }
        }
        println()
      }
    } finally conn.close()
  }
}
