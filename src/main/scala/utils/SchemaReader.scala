package utils

import scala.io.Source

object SchemaReader {

  /** Reads a schema file where each line has the format:
   *   rel_name,col_name1,...,col_namem
   * (same number of columns m for every table)
   */
  def readSchema(filePath: String, defaultType: ColumnType = IntType): Schema = {
    val source = Source.fromFile(filePath)
    try {
      val lines = source.getLines().map(_.trim).filter(_.nonEmpty).toList

      val tables = lines.map { line =>
        val parts = line.split(",").map(_.trim)
        require(parts.length >= 2, s"Malformed line (need relation name + at least one column): $line")

        val relName = parts(0)
        val colNames = parts.drop(1)
        val columns = colNames.map(name => Column(name, defaultType)).toList

        Table(relName, columns)
      }

      // Sanity check: same number of columns m for every table, as you described
      val colCounts = tables.map(_.columns.size).distinct
      require(
        colCounts.size == 1,
        s"Expected all tables to have the same number of columns, but found different counts: $colCounts"
      )

      Schema(tables)
    } finally {
      source.close()
    }
  }
}
