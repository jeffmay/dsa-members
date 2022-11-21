package zio
package csv

import scala.util.control.NonFatal

// TODO: Move to separate files or move Cell to here?

private[csv] object CsvEnv {

  def getOption[T : Tag](env: ZEnvironment[Any]): Option[T] =
    Unsafe.unsafe { implicit unsafe =>
      try Some(env.unsafe.get(Tag[HeaderCtx].tag))
      catch {
        case NonFatal(_) => None
      }
    }
}

private[csv] trait CsvEnv[EnvMin, +Env <: EnvMin] extends Any { self =>
//  protected type EnvMin
//  protected type Env <: EnvMin
//  protected type Self[+env <: EnvMin] <: CsvEnv {
//    type EnvMin = self.EnvMin
//    type Env = env
//  }

  protected type Self[+env <: EnvMin] <: CsvEnv[EnvMin, env]

  def toEnv: ZEnvironment[Env]

  protected def build[R <: EnvMin](env: ZEnvironment[R]): Self[R]

  protected def getOption[T : Tag]: Option[T] = CsvEnv.getOption[T](toEnv)
}

trait RowInfo[EnvMin <: RowCtx, +Env <: EnvMin]
  extends Any with CsvEnv[EnvMin, Env] { self =>
  override protected type Self[+env <: EnvMin] <: RowInfo[EnvMin, env]

  def rowIndex: Long = toEnv.get[RowCtx].rowIndex
}

trait HeaderInfo[EnvMin, +Env <: EnvMin] extends Any with CsvEnv[EnvMin, Env] {
//  override protected type Self[+env <: EnvMin] <: HeaderInfo {
//    type EnvMin = self.EnvMin
//    type Env <: env
//
  //  }

  override protected type Self[+env <: EnvMin] <: HeaderInfo[EnvMin, env]

  def headerContext[H >: Env](implicit
    ev: H <:< HeaderCtx,
    tag: Tag[H],
  ): HeaderCtx =
    ev(toEnv.get[H])

  def optionalHeaderContext: Option[HeaderCtx] = getOption[HeaderCtx]

  def addHeaderContext(headerCtx: HeaderCtx): Self[HeaderCtx with Env] =
    build(toEnv.add[HeaderCtx](headerCtx))
}

type AnyRow = Row[Any]
type RowWithColNames = Row[HeaderCtx]

final case class Row[+H](toEnv: ZEnvironment[H with RowCtx])
  extends AnyVal
  with HeaderInfo[RowCtx, H with RowCtx]
  with RowInfo[RowCtx, H with RowCtx] {
//  override protected type EnvMin = RowCtx
//  override protected type Env = H with RowCtx
  override protected type Self[+env <: RowCtx] = Row[env]

  override protected def build[NR <: RowCtx](env: ZEnvironment[NR]): Row[NR] =
    Row[NR](toEnv.prune[RowCtx].unionAll[NR](env))

  def context: RowCtx = toEnv.get[RowCtx]

  def cells: UIO[Chunk[Cell[H]]] = ZIO.succeed {
    val row = toEnv.get[RowCtx]
    row.cellContents.zipWithIndex.map { case (content, idx) =>
      Cell[H](toEnv.add[CellCtx](CellCtx(idx, content)))
    }
  }

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

  def cell[C >: H : Tag](key: String)(implicit
    ev: C <:< HeaderCtx,
  ): IO[DecodingFailure, Cell[H]] =
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

  def cellAs[T]: Row.GetAsPartiallyApplied[H, T] =
    new Row.GetAsPartiallyApplied[H, T](toEnv)
}

object Row {

  class GetAsPartiallyApplied[+H, T] private[Row] (
    private val env: ZEnvironment[H with RowCtx],
  ) extends AnyVal {

    def apply(idx: Int)(implicit
      decoder: CellDecoder[T],
    ): IO[DecodingFailure, T] =
      Row(env).cell(idx).flatMap(_.contentAs[T])

    def apply[C >: H : Tag](key: String)(implicit
      decoder: CellDecoder[T],
      ev: C <:< HeaderCtx,
    ): IO[DecodingFailure, T] =
      Row(env).cell[C](key).flatMap(_.contentAs[T])
  }
}
