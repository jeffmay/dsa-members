package zio
package csv

object Row {
  type MinCtx = Has[RowCtx] with Has[MaybeHeaderCtx]

  def noHeaderCtx(rowIdx: Long, values: Iterable[String]): Row[Has[NoHeaderCtx]] =
    new Row(Has.allOf[RowCtx, MaybeHeaderCtx, NoHeaderCtx](
      RowCtx(rowIdx, values.toIndexedSeq),
      HeaderCtx.none.widen,
      HeaderCtx.none,
    ))

  def withHeaderCtx(
    rowIdx: Long,
    values: Iterable[String],
    header: HeaderCtx,
  ): Row[Has[HeaderCtx]] =
    new Row(Has.allOf[RowCtx, MaybeHeaderCtx, HeaderCtx](
      RowCtx(rowIdx, values.toIndexedSeq),
      header.widen,
      header,
    ))
}

final class Row[+H] private (
  private val ctx: Row.MinCtx with H,
) extends AnyVal {

  def getHeaderContext[C](implicit
    ev: H <:< Has[C],
    tagged: Tag[C],
  ): C = ctx.get[C]

  def setHeaderContext[C <: MaybeHeaderCtx : Tag](headerCtx: C): Row[Has[C]] =
    new Row[Has[C]](Has.allOf[RowCtx, MaybeHeaderCtx, C](
      ctx.get[RowCtx],
      headerCtx.widen,
      headerCtx,
    ))

  def rowContext: RowCtx = ctx.get[RowCtx]

  def rowIndex: Long = ctx.get[RowCtx].rowIndex

  def cells: IndexedSeq[String] = ctx.get[RowCtx].cells

  def apply(idx: Int): Cell =
    Cell.fromEffect {
      val row = ctx.get[RowCtx]
      ZIO.fromEither {
        // build an option of our cell
        Option.when(row.cells.isDefinedAt(idx)) {
          // merge the cell context with the row context
          Has.allOf(
            row,
            CellCtx(idx, row.cells(idx)),
            ctx.get[MaybeHeaderCtx],
          )
        }.toRight {
          InvalidColumnIndex(row.rowIndex, idx)
        }
      }
    }

  def apply(
    key: String,
  )(implicit
    evH: H <:< Has[HeaderCtx],
    tagged: Tag[HeaderCtx],
  ): Cell =
    Cell.fromEffect {
      for {
        // grab the header context so we can look up the column index by name
        // get the column index or fail
        colIdx <- ZIO.fromEither {
          ctx.get[HeaderCtx].columnIndexByName.get(key).toRight {
            InvalidColumnName(rowIndex, key)
          }
        }
        // reuse the logic above to create our underlying
        cellCtx <- this.apply(colIdx).asEnv
      } yield cellCtx
    }
}
