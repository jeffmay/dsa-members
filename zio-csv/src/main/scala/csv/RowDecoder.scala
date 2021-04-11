package zio
package csv

trait RowDecoder[-R, A] {
  def decode(row: Row): RowDecoder.Result[R, A]
}

object RowDecoder {
  type FromPositionOnly[A] = RowDecoder[Any, A]
  final object FromPositionOnly {
    @inline def apply[A : FromPositionOnly]: FromPositionOnly[A] = implicitly
  }

  type FromHeaderInfo[A] = RowDecoder[Has[HeaderInfo], A]
  final object FromHeaderInfo {
    @inline def apply[A : FromHeaderInfo]: FromHeaderInfo[A] = implicitly
  }

  type MinCtx = Has[RowCtx]
  type Result[-R, A] = ZIO[R with MinCtx, DecodingFailure, A]

  @inline def apply[R, A](implicit
    decoder: RowDecoder[R, A],
  ): RowDecoder[R, A] = decoder
}
