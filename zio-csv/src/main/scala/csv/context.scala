package zio
package csv

final case class HeaderInfo(columns: Map[String, Int])

object HeaderInfo {
  def fromRow(headerRow: Row): HeaderInfo =
    new HeaderInfo(headerRow.cells.zipWithIndex.toMap)
}

final case class RowCtx(rowIndex: Long)

final case class CellCtx(columnIndex: Int, content: String)
