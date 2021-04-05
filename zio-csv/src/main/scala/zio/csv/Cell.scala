package zio.csv

import zio.{Has, URIO, ZIO}

object Cell {

  def fromEffect[R](result: ZIO[R, DecodingFailure, Has[CellCtx]]): Cell[R] =
    new Cell(result)
}

final class Cell[R](
  private[csv] val underlying: ZIO[R, DecodingFailure, Has[CellCtx]],
) extends AnyVal {

  def colIndex: ZIO[R, DecodingFailure, Int] =
    underlying.map(_.get[CellCtx].columnIndex)

  // this comes from the surrounding context and not the underlying ZIO, but it is here for convenience
  def rowIndex: URIO[Has[RowCtx], Long] = ZIO.service[RowCtx].map(_.rowIndex)

  def asString: ZIO[R, DecodingFailure, String] =
    underlying.map(_.get[CellCtx].content)

  def as[A](implicit
    decoder: CellDecoder[A],
  ): ZIO[R with Has[RowCtx], DecodingFailure, A] = {
    for {
      ctx ← underlying
      a ← CellDecoder[A]
        .decodeString(ctx.get[CellCtx].content).provideSome[R with Has[RowCtx]] {
          // provide the resolved cell context as the environment for the decoder
          // the remaining context must come from outside the cell (i.e. the row context and any header context)
          _.union(ctx)
        }
    } yield a
  }
}
