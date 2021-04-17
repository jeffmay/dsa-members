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

  type FromHeaderInfo[A] = RowDecoder[Has[HeaderCtx], A]
  final object FromHeaderInfo {
    @inline def apply[A : FromHeaderInfo]: FromHeaderInfo[A] = implicitly
  }

  implicit class FromHeaderOps[A](private val decoder: FromHeaderInfo[A])
    extends AnyVal {
    def withFixedHeader(header: HeaderCtx): FromPositionOnly[A] = { row ⇒
      decoder.decode(row).provide(Has(header))
    }
  }

  type Result[-R, A] = ZIO[R, DecodingFailure, A]

  @inline def apply[R, A](implicit
    decoder: RowDecoder[R, A],
  ): RowDecoder[R, A] = decoder
}
