package zio
package csv

// TODO: Accumulate errors in row result

trait RowDecoder[-R, +A] {
  def decode(row: Row[R]): RowDecoder.Result[A]
}

object RowDecoder {
  type FromPositionOnly[+A] = RowDecoder[Any, A]
  final object FromPositionOnly {
    @inline def apply[A : FromPositionOnly]: FromPositionOnly[A] = implicitly
  }

  type FromHeaderInfo[+A] = RowDecoder[HeaderCtx, A]
  final object FromHeaderInfo {
    @inline def apply[A : FromHeaderInfo]: FromHeaderInfo[A] = implicitly
  }

  /** Build a [[RowDecoder]] with a given function, rather than rely on converting
    * from a single abstract method (SAM).
    *
    * This is helpful for getting better compiler error messages when variance would
    * make your row or result type not match the expected return type.
    */
  def build[R, A](
    decoder: Row[R] => RowDecoder.Result[A],
  ): RowDecoder[R, A] = decoder.apply

  implicit def decodeEither[R, A](implicit
    decoder: RowDecoder[R, A],
  ): RowDecoder[R, Either[DecodingFailure, A]] = { row =>
    decoder.decode(row).either
  }

  implicit def decodeOptional[R, A](implicit
    decoder: RowDecoder[R, A],
  ): RowDecoder[R, Option[A]] = { row =>
    decoder.decode(row).either.map(_.toOption)
  }

  implicit class FromHeaderOps[+A](private val decoder: FromHeaderInfo[A])
    extends AnyVal {
    def withFixedHeader(header: HeaderCtx): FromPositionOnly[A] = { row =>
      val env = row.toEnv.add(header)
      decoder.decode(Row[HeaderCtx](env))
    }
  }

  type Result[+A] = IO[DecodingFailure, A]

  @inline def apply[R, A](implicit
    decoder: RowDecoder[R, A],
  ): RowDecoder[R, A] = decoder
}
