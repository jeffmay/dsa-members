package zio
package csv

object Row {
  def fromIterable(values: Iterable[String]): Row =
    new Row(values.toArray[String])
}

// TODO: Allow passing the RowCtx somehow... maybe subclasses with different environment requirements?
final class Row private (
  val cells: IndexedSeq[String],
) extends AnyVal {

  def apply(idx: Int): Cell[Has[RowCtx]] = Cell.fromEffect {
    ZIO.fromOption {
      // build an option of our cell
      Option.when(cells.isDefinedAt(idx)) {
        CellCtx(idx, cells(idx))
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
  ): Cell[Has[HeaderInfo] with Has[RowCtx]] = Cell.fromEffect {
    for {
      // grab the header context so we can look up the column index by name
      header ← ZIO.service[HeaderInfo]
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
