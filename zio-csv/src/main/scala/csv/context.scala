package zio
package csv

/** The [[HeaderCtx]] is available in scope at this point, and can be used for looking up column names or indexes.
  */
final case class HeaderCtx(headerRow: Chunk[String]) {

  lazy val columnIndexByName: Map[String, Int] =
    headerRow.zipWithIndex.toMap

  lazy val columnNameByIndex: Map[Int, String] =
    headerRow.indices.zip(headerRow).toMap
}

object HeaderCtx {

  def fromHeaderRow(headerRow: IterableOnce[String]): HeaderCtx =
    new HeaderCtx(Chunk.from(headerRow))
}

final case class RowCtx(rowIndex: Long, cellContents: Chunk[String])

final case class CellCtx(
  columnIndex: Int,
  content: String,
)
