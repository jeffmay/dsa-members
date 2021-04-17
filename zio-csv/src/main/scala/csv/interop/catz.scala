package zio
package csv.interop

import csv.CellDecoder

import cats.Alternative

object catz extends CatsPlatform

trait CatsPlatform extends CatsCellDecoderInstances

trait CatsCellDecoderInstances {
  import cats.syntax.apply._
  import zio.interop.catz.core._

  implicit val alternative: Alternative[CellDecoder] =
    new Alternative[CellDecoder] {

      override def pure[A](x: A): CellDecoder[A] = CellDecoder.const(x)

      override def ap[A, B](
        ff: CellDecoder[A ⇒ B],
      )(fa: CellDecoder[A]): CellDecoder[B] = { cell ⇒
        (ff.decodeString(cell), fa.decodeString(cell)).mapN {
          _ apply _
        }
      }

      override def empty[A]: CellDecoder[A] =
        CellDecoder.failWithMessage(
          "Empty CellDecoder was not expecting a value",
        )

      override def combineK[A](
        x: CellDecoder[A],
        y: CellDecoder[A],
      ): CellDecoder[A] = x.or(y)
    }
}
