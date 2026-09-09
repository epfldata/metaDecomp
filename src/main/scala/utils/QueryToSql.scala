package utils

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.Random

/** Converts a comma-separated query description into a SQL SELECT statement.
  *
  * Each non-empty input line must have the form:
  *
  *   table_name,attribute1_name,attribute2_name,...
  *
  * Usage: QueryToSql <input-file> <number-of-selected-columns>
  */
object QueryToSql {

  final case class Table(name: String, attributes: Vector[String])

  def readQuery(path: Path): Vector[Table] = {
    val tables = Files.readAllLines(path, StandardCharsets.UTF_8).asScala
      .zipWithIndex
      .flatMap { case (rawLine, index) =>
        val line = rawLine.trim
        if (line.isEmpty) {
          None
        } else {
          val fields = line.split(",", -1).map(_.trim).toVector
          if (fields.length < 2 || fields.exists(_.isEmpty)) {
            throw new IllegalArgumentException(
              s"Invalid input at line ${index + 1}: expected a table and at least one attribute"
            )
          }
          Some(Table(fields.head, fields.tail.distinct))
        }
      }
      .toVector

    if (tables.isEmpty) {
      throw new IllegalArgumentException("The input file contains no table definitions")
    }

    val duplicateTables = tables.groupBy(_.name).collect {
      case (name, definitions) if definitions.size > 1 => name
    }
    if (duplicateTables.nonEmpty) {
      throw new IllegalArgumentException(
        s"Each table must occur once; duplicate tables: ${duplicateTables.toVector.sorted.mkString(", ")}"
      )
    }

    tables
  }

  def toSql(tables: Vector[Table], selectedColumnCount: Int, random: Random = new Random()): String = {
    val columns = tables.flatMap(table => table.attributes.map(attribute => s"${table.name}.$attribute"))
    require(selectedColumnCount >= 0, "C must be non-negative")
    require(
      selectedColumnCount <= columns.size,
      s"C ($selectedColumnCount) exceeds the number of available columns (${columns.size})"
    )

    val selectedColumns = random.shuffle(columns).take(selectedColumnCount)

    // LinkedHashMap keeps predicates deterministic according to their first input occurrence.
    val tablesByAttribute = mutable.LinkedHashMap.empty[String, Vector[String]]
    tables.foreach { table =>
      table.attributes.foreach { attribute =>
        tablesByAttribute.update(
          attribute,
          tablesByAttribute.getOrElse(attribute, Vector.empty) :+ table.name
        )
      }
    }

    val predicates = tablesByAttribute.toVector.flatMap { case (attribute, tableNames) =>
      for {
        firstIndex <- tableNames.indices
        secondIndex <- (firstIndex + 1) until tableNames.length
      } yield s"${tableNames(firstIndex)}.$attribute = ${tableNames(secondIndex)}.$attribute"
    }

    val selectClause = if (selectedColumns.isEmpty) "SELECT *" else s"SELECT ${selectedColumns.mkString(", ")}"
    val fromClause = s"FROM ${tables.map(_.name).mkString(", ")}"
    val whereClause = if (predicates.isEmpty) "" else s"\nWHERE ${predicates.mkString(" AND\n      ")}"

    s"$selectClause\n$fromClause$whereClause;\n"
  }

  def outputPath(inputPath: Path): Path = {
    val fileName = inputPath.getFileName.toString
    val sqlFileName =
      if (fileName.endsWith(".txt")) fileName.stripSuffix(".txt") + ".sql"
      else fileName + ".sql"
    Option(inputPath.getParent).map(_.resolve(sqlFileName)).getOrElse(Paths.get(sqlFileName))
  }

  def convert(inputPath: Path, selectedColumnCount: Int): Path = {
    val destination = outputPath(inputPath)
    val sql = toSql(readQuery(inputPath), selectedColumnCount)
    Files.writeString(destination, sql, StandardCharsets.UTF_8)
    destination
  }

  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      throw new IllegalArgumentException("Usage: QueryToSql <input-file> <C>")
    }

    val selectedColumnCount = args(1).toIntOption.getOrElse {
      throw new IllegalArgumentException(s"C must be an integer, but was '${args(1)}'")
    }
    val destination = convert(Paths.get(args(0)), selectedColumnCount)
    println(s"Wrote SQL query to $destination")
  }
}
