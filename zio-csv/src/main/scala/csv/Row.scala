package zio
package csv

object Row {
  def apply(rowIdx: Long, values: Iterable[String]): Row =
    new Row(RowCtx(rowIdx, values.toArray[String]))
}

// TODO: Allow passing the RowCtx somehow... maybe subclasses with different environment requirements?
final class Row private (
  val ctx: RowCtx,
) extends AnyVal {

  def rowIndex: Long = ctx.rowIndex

  def cells: IndexedSeq[String] = ctx.cells

  def apply(idx: Int): Cell[Any] = Cell.fromEffect {
    ZIO.fromEither {
      // build an option of our cell
      Option.when(cells.isDefinedAt(idx)) {
        // merge the cell context with the row context
        Has.allOf(ctx, CellCtx(idx, cells(idx)))
      }.toRight {
        InvalidColumnIndex(rowIndex, idx)
      }
    }
  }

  def apply(
    key: String,
  ): Cell[Has[HeaderCtx]] = Cell.fromEffect {
    for {
      // grab the header context so we can look up the column index by name
      header ← ZIO.service[HeaderCtx]
      // get the column index or fail
      colIdx ← ZIO.fromEither {
        header.columns.get(key).toRight {
          InvalidColumnName(rowIndex, key)
        }
      }
      // reuse the logic above to create our underlying
      cellCtx ← apply(colIdx).underlying
    } yield cellCtx
  }
}
