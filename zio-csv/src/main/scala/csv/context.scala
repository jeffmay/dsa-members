package zio
package csv

final case class HeaderCtx(headerRow: IndexedSeq[String]) {

  lazy val columnIndexByName: Map[String, Int] =
    headerRow.zip(headerRow.indices).toMap

  lazy val columnNameByIndex: Map[Int, String] =
    headerRow.indices.zip(headerRow).toMap
}

object HeaderCtx {
  def fromCells(headerRow: IndexedSeq[String]): HeaderCtx =
    new HeaderCtx(headerRow)
}

final case class RowCtx(rowIndex: Long, cells: IndexedSeq[String])

final case class CellCtx(
  columnIndex: Int,
  columnName: Option[String],
  content: String,
)
