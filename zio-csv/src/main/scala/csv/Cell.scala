package zio
package csv

object Cell {
  type Env = Has[RowCtx] with Has[CellCtx]

  def fromEffect[R](
    result: ZIO[R, DecodingFailure, Env],
  ): Cell[R] =
    new Cell(result)
}

final class Cell[-R](
  private[csv] val underlying: ZIO[R, DecodingFailure, Cell.Env],
) extends AnyVal {

  def colIndex: ZIO[R, DecodingFailure, Int] =
    underlying.map(_.get[CellCtx].columnIndex)

  def rowIndex: ZIO[R, DecodingFailure, Long] =
    underlying.map(_.get[RowCtx].rowIndex)

  def asString: ZIO[R, DecodingFailure, String] =
    underlying.map(_.get[CellCtx].content)

  def as[A](implicit
    decoder: CellDecoder[A],
  ): ZIO[R, DecodingFailure, A] = {
    for {
      ctx ← underlying
      a ← CellDecoder[A]
        // provide the resolved cell context as the environment for the decoder
        // the remaining context must come from outside the cell (i.e. the header context)
        .decodeString(ctx.get[CellCtx].content).provide(ctx)
    } yield a
  }
}
