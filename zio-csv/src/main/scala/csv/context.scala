package zio
package csv

final case class HeaderCtx(columns: Map[String, Int])
object HeaderCtx {
  def apply(row: Row): HeaderCtx = apply(row.cells)
  def apply(row: Seq[String]): HeaderCtx =
    new HeaderCtx(row.zipWithIndex.toMap)
}

final case class RowCtx(rowIndex: Long)

final case class CellCtx(columnIndex: Int, content: String)
