package zio
package csv

object Cell {
  type Env = Has[RowCtx] with Has[CellCtx]

  /** Creates a single [[Cell]] inside an [[Env]] with no other rows or columns.
    */
  def detached(content: String): Cell[Any] =
    fromEffect(ZIO.succeed(detachedEnv(content)))

  /** Creates an [[Env]] containing 1 row and 1 column where the 1 cell contains the given content.
    */
  def detachedEnv(content: String): Env =
    Has(RowCtx(1, Vector(content))).add(CellCtx(0, None, content))

  def fromEffect[R](
    result: ZIO[R, DecodingFailure, Env],
  ): Cell[R] =
    new Cell(result)
}

final class Cell[-R](
  val asEnv: ZIO[R, DecodingFailure, Cell.Env],
) extends AnyVal {

  def colIndex: ZIO[R, DecodingFailure, Int] =
    asEnv.map(_.get[CellCtx].columnIndex)

  def rowIndex: ZIO[R, DecodingFailure, Long] =
    asEnv.map(_.get[RowCtx].rowIndex)

  def asString: ZIO[R, DecodingFailure, String] =
    asEnv.map(_.get[CellCtx].content)

  def as[A](implicit
    decoder: CellDecoder[A],
  ): ZIO[R, DecodingFailure, A] = {
    for {
      env <- asEnv
      a <- CellDecoder[A]
        // provide the resolved cell context as the environment for the decoder
        // the remaining context must come from outside the cell (i.e. the header context)
        .decodeString(env.get[CellCtx].content).provide(env)
    } yield a
  }
}
