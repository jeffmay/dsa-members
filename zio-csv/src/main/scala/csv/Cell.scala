package zio
package csv

object Cell {
  type Env = Has[RowCtx] with Has[CellCtx] with Has[MaybeHeaderCtx]

  /** Creates a single [[Cell]] inside an [[Env]] with no other rows or columns.
    */
  def detached(content: String): Cell =
    fromEffect(ZIO.succeed(detachedEnv(content)))

  /** Creates an [[Env]] containing 1 row and 1 column where the 1 cell contains the given content.
    */
  def detachedEnv(content: String): Env =
    Has.allOf[RowCtx, CellCtx, MaybeHeaderCtx](
      RowCtx(1, Vector(content)),
      CellCtx(0, content),
      HeaderCtx.none,
    )

  def fromEffect(
    result: IO[DecodingFailure, Env],
  ): Cell =
    new Cell(result)
}

final class Cell(
  val asEnv: IO[DecodingFailure, Cell.Env],
) extends AnyVal {

  def colIndex: IO[DecodingFailure, Int] =
    asEnv.map(_.get[CellCtx].columnIndex)

  def rowIndex: IO[DecodingFailure, Long] =
    asEnv.map(_.get[RowCtx].rowIndex)

  def asString: IO[DecodingFailure, String] =
    asEnv.map(_.get[CellCtx].content)

  def as[A](implicit
    decoder: CellDecoder[A],
  ): IO[DecodingFailure, A] = {
    for {
      env <- asEnv
      a <- CellDecoder[A]
        // provide the resolved cell context as the environment for the decoder
        // the remaining context must come from outside the cell (i.e. the header context)
        .decodeString(env.get[CellCtx].content).provide(env)
    } yield a
  }
}
