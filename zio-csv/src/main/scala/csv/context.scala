package zio
package csv

final case class HeaderCtx(columns: Map[String, Int])

object HeaderCtx {
  def fromRow(headerRow: Row): HeaderCtx =
    new HeaderCtx(headerRow.cells.zipWithIndex.toMap)
}

final case class RowCtx(rowIndex: Long, cells: IndexedSeq[String])

final case class CellCtx(columnIndex: Int, content: String)
