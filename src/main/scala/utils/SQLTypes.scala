package utils

sealed trait ColumnType
case object IntType extends ColumnType
case object DoubleType extends ColumnType
case object StringType extends ColumnType
case object BoolType extends ColumnType

case class Column(name: String, colType: ColumnType, nullable: Boolean = false)
case class Table(name: String, columns: List[Column])

case class Schema(tables: List[Table])