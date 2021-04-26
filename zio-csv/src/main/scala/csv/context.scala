package zio
package csv

/** Represents the possibility of having column information from the header row.
  */
sealed abstract class MaybeHeaderCtx {
  def maybeHeaderCtx: Option[HeaderCtx]
  final def widen: MaybeHeaderCtx = this
}

/** No context from the header is available in scope at this point.
  */
case object NoHeaderCtx extends NoHeaderCtx
sealed abstract class NoHeaderCtx extends MaybeHeaderCtx {
  override def maybeHeaderCtx: Option[HeaderCtx] = None
}

/** The [[HeaderCtx]] is available in scope at this point, and can be used for looking up column names or indexes.
  */
final case class HeaderCtx(headerRow: IndexedSeq[String])
  extends MaybeHeaderCtx {

  override def maybeHeaderCtx: Option[HeaderCtx] = Some(this)

  lazy val columnIndexByName: Map[String, Int] =
    headerRow.zip(headerRow.indices).toMap

  lazy val columnNameByIndex: Map[Int, String] =
    headerRow.indices.zip(headerRow).toMap
}

object HeaderCtx {

  def none: NoHeaderCtx = NoHeaderCtx

  def fromHeaderRow(headerRow: IndexedSeq[String]): HeaderCtx =
    new HeaderCtx(headerRow)
}

final case class RowCtx(rowIndex: Long, cells: IndexedSeq[String])

final case class CellCtx(
  columnIndex: Int,
  content: String,
)
