package io.eels

case class FrameSchema(columns: Seq[Column]) {

  def addColumn(col: Column): FrameSchema = copy(columns :+ col)

  def removeColumn(name: String): FrameSchema = copy(columns = columns.filterNot(_.name == name))

  def join(other: FrameSchema): FrameSchema = {
    require(
      columns.map(_.name).intersect(other.columns.map(_.name)).isEmpty,
      "Cannot join two frames which have duplicated column names"
    )
    FrameSchema(columns ++ other.columns)
  }

  def print: String = {
    columns.map { column =>
      val str = s"- ${column.name} [${column.`type`}]"
      if (column.nullable) str + " (nullable)"
      else str
    }.mkString("\n")
  }
}