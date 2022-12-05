package zio
package csv.interop

import csv.CellDecoder.MinCtx
import csv.{CellDecoder, CellDecodingFailure, ReadingFailure}
import interop.catz.core._

import cats.{Eq, MonadError, SemigroupK}

object catz extends CatsPlatform

trait CatsPlatform
  extends CatsCellDecoderInstances with CatsDecodingFailureInstances

trait CatsCellDecoderInstances {

  implicit val cellDecoderSemigroupK: SemigroupK[CellDecoder] =
    new CatsCellDecoderSemigroupK

  implicit val cellDecoderMonadError: MonadError[
    CellDecoder,
    CellDecodingFailure,
  ] = new CatsCellDecoderMonadError

}

trait CatsDecodingFailureInstances {

  implicit def eqReadingFailure[E <: ReadingFailure]: Eq[E] = EqProduct[E]
}

private class CatsCellDecoderSemigroupK extends SemigroupK[CellDecoder] {
  final override def combineK[A](
    x: CellDecoder[A],
    y: CellDecoder[A],
  ): CellDecoder[A] = x.or(y)
}

final private class CatsCellDecoderMonadError
  extends MonadError[CellDecoder, CellDecodingFailure] {

  override def map[A, B](fa: CellDecoder[A])(f: A => B): CellDecoder[B] =
    fa.map(f)

  override def product[A, B](
    fa: CellDecoder[A],
    fb: CellDecoder[B],
  ): CellDecoder[(A, B)] = fa.product(fb)

  override def ap[A, B](
    ff: CellDecoder[A => B],
  )(fa: CellDecoder[A]): CellDecoder[B] = ff.product(fa).map {
    case (f, a) => f(a)
  }

  override def ap2[A, B, Z](ff: CellDecoder[(A, B) => Z])(
    fa: CellDecoder[A],
    fb: CellDecoder[B],
  ): CellDecoder[Z] =
    ff.product(fa.product(fb)).map {
      case (f, (a, b)) => f(a, b)
    }

  override def map2[A, B, Z](
    fa: CellDecoder[A],
    fb: CellDecoder[B],
  )(f: (A, B) => Z): CellDecoder[Z] =
    fa.product(fb).map {
      case (a, b) => f(a, b)
    }

  override def productR[A, B](fa: CellDecoder[A])(
    fb: CellDecoder[B],
  ): CellDecoder[B] = fa.product(fb).map(_._2)

  override def productL[A, B](fa: CellDecoder[A])(
    fb: CellDecoder[B],
  ): CellDecoder[A] = fa.product(fb).map(_._1)

  override def flatMap[A, B](
    fa: CellDecoder[A],
  )(f: A => CellDecoder[B]): CellDecoder[B] = fa.flatMap(f)

  override def handleErrorWith[A](
    fa: CellDecoder[A],
  )(f: CellDecodingFailure => CellDecoder[A]): CellDecoder[A] = { content =>
    fa.decodeString(content).catchAll(f.andThen(_.decodeString(content)))
  }

  override def pure[A](x: A): CellDecoder[A] = CellDecoder.const(x)

  override def tailRecM[A, B](
    a: A,
  )(f: A => CellDecoder[Either[A, B]]): CellDecoder[B] = { content =>
    MonadError[
      ZIO[MinCtx, CellDecodingFailure, _],
      CellDecodingFailure,
    ].tailRecM[A, B](a)(f.andThen(_.decodeString(content)))
  }

  override def raiseError[A](e: CellDecodingFailure): CellDecoder[A] =
    CellDecoder.fail(e)
}

/** Defines [[Eq]] by trusting that the definition of [[Equals]] from
  * [[Product]] is correct.
  */
case object EqProduct extends Eq[Product] {
  override def eqv(x: Product, y: Product): Boolean = x == y
  inline def apply[P <: Product]: Eq[P] = this.asInstanceOf[Eq[P]]
}
