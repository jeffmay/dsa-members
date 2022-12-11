package zio
package csv

/** A row with the minimum available context. With this type of row, you cannot
  * lookup columns by name.
  */
type AnyRow = Row[Any]

/** A row from CSV data that has a header row. Used to lookup columns by name
  * instead of just index.
  */
type RowWithColNames = Row[HeaderCtx]

/** The interface for operating on a given row of a CSV file.
  *
  * @see
  *   [[CsvEnv]] for more details about this pattern of wrapping a
  *   [[ZEnvironment]]
  *
  * @param toEnv
  *   the environment that contains the current context about the CSV data and
  *   current row that is being decoded.
  */
final case class Row[+H](toEnv: ZEnvironment[H & RowCtx])
  extends AnyVal
  with HeaderInfo[RowCtx, H & RowCtx]
  with RowInfo[RowCtx, H & RowCtx] {
  override protected type Self[+env <: RowCtx] = Row[env]

  override protected def build[NR <: RowCtx](env: ZEnvironment[NR]): Row[NR] =
    Row[NR](toEnv.prune[RowCtx].unionAll[NR](env))

  /** @return
    *   the [[RowCtx]] of this row.
    */
  def context: RowCtx = toEnv.get[RowCtx]

  /** Update the RowCtx with the given function. */
  def mapContext(fn: RowCtx => RowCtx): Row[H] =
    new Row(toEnv.update[RowCtx](fn))

  /** @return
    *   all the cells of this row, wrapped with the [[Cell]] interface.
    */
  def cells: UIO[Chunk[Cell[H]]] = ZIO.succeed {
    val row = toEnv.get[RowCtx]
    row.cellContents.zipWithIndex.map { case (content, idx) =>
      Cell[H](toEnv.add[CellCtx](CellCtx(idx, content)))
    }
  }

  /** @return
    *   a single cell of this row with the given index.
    */
  def cell(idx: Int): IO[InvalidColumnIndex, Cell[H]] = cells.flatMap {
    cells =>
      // build an option of our cell
      if (cells.isDefinedAt(idx)) {
        // merge the cell context with the row context
        ZIO.succeed(cells.apply(idx))
      } else {
        ZIO.fail(InvalidColumnIndex(rowIndex, idx))
      }
  }

  /** Returns the column by name or fails with [[InvalidColumnIndex]] or
    * [[InvalidColumnName]].
    *
    * @param key
    *   the name of the column
    * @param ev
    *   proof that this row has access to [[HeaderCtx]]
    * @return
    *   a single cell of this row with the index associated with the given
    *   column name.
    */
  def cell[C >: H : Tag](key: String)(implicit
    ev: C <:< HeaderCtx,
  ): IO[InvalidColumnName | InvalidColumnIndex, Cell[H]] =
    for {
      // grab the header context so we can look up the column index by name
      // get the column index or fail
      colIdx <- ZIO.fromEither {
        ev(toEnv.get[C]).columnIndexByName.get(key).toRight {
          InvalidColumnName(rowIndex, key)
        }
      }
      // reuse the logic above to create our underlying
      cellCtx <- cell(colIdx)
    } yield cellCtx

  /** Read a cell and decode it as the given type.
    *
    * @see
    *   [[Row.GetAsPartiallyApplied]] for how to apply the cell selector (by
    *   name or index).
    */
  def cellAs[T]: Row.GetAsPartiallyApplied[H, T] =
    new Row.GetAsPartiallyApplied[H, T](toEnv)
}

object Row {

  def apply(index: Long, cells: IterableOnce[String]): Row[Any] =
    Row[Any](ZEnvironment(RowCtx(index, Chunk.from(cells))))

  inline def withHeaderContext(
    header: HeaderCtx,
    index: Long,
    cells: IterableOnce[String],
  ): Row[HeaderCtx] =
    withEnvironment(ZEnvironment(header), index, cells)

  def withEnvironment[R](
    env: ZEnvironment[R],
    index: Long,
    cells: IterableOnce[String],
  ): Row[R] =
    Row[R](env.add(RowCtx(index, Chunk.from(cells))))

  class GetAsPartiallyApplied[+H, T] private[Row] (
    private val env: ZEnvironment[H & RowCtx],
  ) extends AnyVal {

    /** Select a given cell by index and decode as the partially applied type
      * [[T]].
      *
      * @param idx
      *   the index of the current row to read
      * @param decoder
      *   the [[CellDecoder]] to apply to the row (if it is not an invalid
      *   column index)
      * @return
      *   a ZIO of either a [[DecodingFailure]] or the expected cell value
      */
    def apply(idx: Int)(implicit
      decoder: CellDecoder[T],
    ): IO[InvalidColumnIndex | CellDecodingFailure, T] =
      Row(env).cell(idx).flatMap(_.contentAs[T])

    /** Select a given cell by column name and decode as the partially applied
      * type [[T]].
      *
      * @param key
      *   the column name of the current row to read (based on the available
      *   [[HeaderCtx]])
      * @param decoder
      *   the [[CellDecoder]] to apply to the row (if it is not an invalid
      *   column name or index)
      * @return
      *   a ZIO of either a [[DecodingFailure]] or the expected cell value
      */
    def apply[C >: H : Tag](key: String)(implicit
      decoder: CellDecoder[T],
      ev: C <:< HeaderCtx,
    ): IO[InvalidColumnName | InvalidColumnIndex | CellDecodingFailure, T] =
      Row(env).cell[C](key).flatMap(_.contentAs[T])
  }
}

// TODO: Add the line number to the row context

/** Context about the current row carried around in the [[ZEnvironment]] of the
  * [[RowDecoder]].
  *
  * @param rowIndex
  *   the index of the current row
  * @param cellContents
  *   the raw contents of the row as parsed
  */
final case class RowCtx(rowIndex: Long, cellContents: Chunk[String])
