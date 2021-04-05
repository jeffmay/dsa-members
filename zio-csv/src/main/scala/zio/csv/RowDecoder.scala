package zio.csv

import zio.{Has, ZIO}

trait RowDecoder[-R, A] {
  def decode(row: Row): RowDecoder.Result[R, A]
}

object RowDecoder {
  type FromPositionOnly[A] = RowDecoder[Any, A]
  type FromHeaderCtx[A] = RowDecoder[Has[HeaderCtx], A]
  type MinCtx = Has[RowCtx]
  type Result[-R, A] = ZIO[R with MinCtx, DecodingFailure, A]

  @inline def apply[R, A](implicit
    decoder: RowDecoder[R, A],
  ): RowDecoder[R, A] = decoder
}
