package zio
package csv
import csv.Cell.Ctx

object Cell {
  type Ctx[+H] = H with CellCtx with RowCtx

  /** Creates an [[Cell]] containing 1 row and 1 column with no header where the 1 cell contains the given content.
    */
  def detached(content: String, rowIndex: Long = 0, colIndex: Int = 0): Cell[Any] = {
    val env = ZEnvironment[RowCtx, CellCtx](
      RowCtx(rowIndex, Chunk(content)),
      CellCtx(colIndex, content),
    )
    Cell(env)
  }

}

final case class Cell[+H](
  toEnv: ZEnvironment[Cell.Ctx[H]],
) extends AnyVal
  with HeaderInfo[Cell.Ctx[Any], Cell.Ctx[H]]
  with RowInfo[Cell.Ctx[Any], Cell.Ctx[H]] {
//  override type Header = H
//  override type EnvMin = Cell.Ctx[Any]
//  override type Env = Cell.Ctx[H]
  override type Self[+env <: Cell.Ctx[Any]] = Cell[env]

  override protected def build[R <: Ctx[Any]](env: ZEnvironment[R]): Cell[R] =
    Cell[R](toEnv.prune[Cell.Ctx[Any]].unionAll[R](env))

  def colIndex: Int = toEnv.get[CellCtx].columnIndex

  def content: String =
    toEnv.get[CellCtx].content

  def as[A](implicit
    decoder: CellDecoder[A],
  ): IO[DecodingFailure, A] = {
    CellDecoder[A]
      // provide the resolved cell context as the environment for the decoder
      // the remaining context must come from outside the cell (i.e. the header context)
      .decodeString(toEnv.get[CellCtx].content).provideEnvironment(toEnv)
  }
}
