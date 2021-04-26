package zio
package csv

object Row {

  def noHeaderCtx(rowIdx: Long, values: Iterable[String]): Row[Any] =
    new Row(Has(RowCtx(rowIdx, values.toIndexedSeq)))

  def withHeaderCtx(
    rowIdx: Long,
    values: Iterable[String],
    header: HeaderCtx,
  ): Row[Has[HeaderCtx]] =
    new Row(Has.allOf[RowCtx, HeaderCtx](
      RowCtx(rowIdx, values.toIndexedSeq),
      header,
    ))
}

final class Row[+A] private (
  private val ctx: Has[RowCtx] with A,
) extends AnyVal {

  def getFromContext[S](implicit ev: A <:< Has[S], tagged: Tag[S]): S =
    ctx.get[S]

  def addToContext[B](context: B): Row[A with Has[B]] = new Row(ctx.add(context))

  def rowContext: RowCtx = ctx.get[RowCtx]

  def rowIndex: Long = ctx.get[RowCtx].rowIndex

  def cells: IndexedSeq[String] = ctx.get[RowCtx].cells

  def apply(idx: Int)(implicit m: MaybeHeaderCtx[A]): Cell[Any] =
    Cell.fromEffect {
      val row = ctx.get[RowCtx]
      ZIO.fromEither {
        // build an option of our cell
        Option.when(row.cells.isDefinedAt(idx)) {
          // merge the cell context with the row context
          ctx.add(
            CellCtx(idx, m.columnName(ctx, idx), row.cells(idx)),
          )
        }.toRight {
          InvalidColumnIndex(row.rowIndex, idx)
        }
      }
    }

  def apply(
    key: String,
  ): Cell[Has[HeaderCtx]] =
    Cell.fromEffect {
      for {
        // grab the header context so we can look up the column index by name
        header <- ZIO.service[HeaderCtx]
        // get the column index or fail
        colIdx <- ZIO.fromEither {
          header.columnIndexByName.get(key).toRight {
            InvalidColumnName(rowIndex, key)
          }
        }
        // reuse the logic above to create our underlying
        cellCtx <- this.apply(colIdx).asEnv
      } yield cellCtx
    }
}
