package utils

import java.sql.{Connection, DriverManager, PreparedStatement}
import scala.util.Random

object RandomDbGenerator {

  private def sqlType(c: ColumnType): String = c match {
    case IntType    => "INTEGER"
    case DoubleType => "DOUBLE"
    case StringType => "VARCHAR"
    case BoolType   => "BOOLEAN"
  }

  private def randomValue(c: ColumnType, rng: Random, intRange: Int = 20): Any = c match {
    case IntType    => rng.nextInt(intRange)
    case DoubleType => math.round(rng.nextDouble() * 10000) / 100.0
    case StringType => randomString(rng, 6 + rng.nextInt(6))
    case BoolType   => rng.nextBoolean()
  }

  private def randomString(rng: Random, len: Int): String = {
    val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    (1 to len).map(_ => chars(rng.nextInt(chars.length))).mkString
  }

  def generate(
                schema: Schema,
                dbPath: String,
                rowsPerTable: Int = 1000,
                intRange: Int = 20,
                nullProbability: Double = 0.05,
                seed: Option[Long] = None
              ): Unit = {
    val rng = seed.map(new Random(_)).getOrElse(new Random())

    Class.forName("org.duckdb.DuckDBDriver")
    val conn: Connection = DriverManager.getConnection(s"jdbc:duckdb:$dbPath")

    try {
      conn.setAutoCommit(false)
      val stmt = conn.createStatement()

      // Create tables
      schema.tables.foreach { table =>
        val colDefs = table.columns
          .map(c => s"${c.name} ${sqlType(c.colType)}")
          .mkString(", ")
        stmt.executeUpdate(s"DROP TABLE IF EXISTS ${table.name}")
        stmt.executeUpdate(s"CREATE TABLE ${table.name} ($colDefs)")
      }
      stmt.close()

      // Insert random rows
      schema.tables.foreach { table =>
        val placeholders = table.columns.map(_ => "?").mkString(", ")
        val insertSql = s"INSERT INTO ${table.name} (${table.columns.map(_.name).mkString(", ")}) VALUES ($placeholders)"
        val ps: PreparedStatement = conn.prepareStatement(insertSql)

        for (_ <- 1 to rowsPerTable) {
          table.columns.zipWithIndex.foreach { case (col, idx) =>
            val isNull = col.nullable && rng.nextDouble() < nullProbability
            if (isNull) {
              ps.setNull(idx + 1, java.sql.Types.NULL)
            } else {
              randomValue(col.colType, rng, intRange) match {
                case v: Int    => ps.setInt(idx + 1, v)
                case v: Double => ps.setDouble(idx + 1, v)
                case v: String => ps.setString(idx + 1, v)
                case v: Boolean => ps.setBoolean(idx + 1, v)
              }
            }
          }
          ps.addBatch()
        }
        ps.executeBatch()
        ps.close()
      }

      conn.commit()
    } finally {
      conn.close()
    }
  }
}
