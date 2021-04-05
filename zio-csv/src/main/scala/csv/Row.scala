package zio
package csv

object Row {
  implicit val equiv: Equiv[Row] = Equiv.by(_.cells)

  // specialized on array to avoid allocation and boxing
  // this is unsafe because it does not copy the mutable array
  @inline private[csv] def unsafeFromArray(values: Array[String]): Row =
    new Row(values)

  def fromIterable(values: Iterable[String]): Row =
    new Row(values.toArray[String])
}

final class Row private (
  private val unsafeArray: Array[String],
) extends AnyVal {

  def cells: IndexedSeq[String] = unsafeArray

  def apply(idx: Int): Cell[Has[RowCtx]] = Cell.fromEffect {
    ZIO.fromOption {
      // build an option of our cell
      Option.when(unsafeArray.isDefinedAt(idx)) {
        CellCtx(idx, unsafeArray(idx))
      }
    }.flatMap { cell ⇒
      // grab the row context from the surrounding context
      ZIO.service[RowCtx].map { row ⇒
        Has.allOf(row, cell)
      }
    }.flatMapError { _ ⇒
      // we need the row context to produce our error as well
      ZIO.service[RowCtx].map { row ⇒
        InvalidColumnIndex(row.rowIndex, idx)
      }
    }
  }

  def apply(
    key: String,
  ): Cell[Has[HeaderCtx] with Has[RowCtx]] = Cell.fromEffect {
    for {
      // grab the header context so we can look up the column index by name
      header ← ZIO.service[HeaderCtx]
      // get the column index or fail
      colIdx ← ZIO.succeed(header.columns.get(key)).some.flatMapError { _ ⇒
        // we need the row context for our error message
        ZIO.service[RowCtx].map { row ⇒
          InvalidColumnName(row.rowIndex, key)
        }
      }
      // reuse the logic above to create our underlying
      cellCtx ← apply(colIdx).underlying
    } yield cellCtx
  }
}
