package zio
package csv.scalacheck

import csv.interop.catz._
import csv._

import cats.Eq
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.rng.Seed
import org.scalacheck.{Arbitrary, Cogen, Gen, Prop, Test}

trait CellDecoderInstances {

  // Move somewhere better?
  protected def codecEqualityCheckCount: Int = 16

  val genGenericCellDecodingFailure: Gen[GenericCellDecodingFailure] =
    Gen.const(GenericCellDecodingFailure(0, 0, "generated"))

  implicit val arbCellDecodingFailure: Arbitrary[CellDecodingFailure] =
    Arbitrary {
      genGenericCellDecodingFailure
    }

  implicit val cogenCellDecodingFailure: Cogen[CellDecodingFailure] =
    Cogen(_.hashCode())

  implicit def arbCellDecoder[A : Arbitrary]: Arbitrary[CellDecoder[A]] = {
    Arbitrary {
      for {
        fn ← arbitrary[String ⇒ Either[CellDecodingFailure, A]]
      } yield CellDecoder.fromEither(fn)
    }
  }

  implicit def eqCellDecoder[A : Arbitrary : Eq](implicit
    seed: Seed,
  ): Eq[CellDecoder[A]] = {
    val runtime = Runtime.default
    (x, y) ⇒ {
      val test = Prop.forAll { a: String ⇒
        val cell = Cell.detached(a)
        val xRes = runtime.unsafeRun(x.decodeCell(cell).either)
        val yRes = runtime.unsafeRun(y.decodeCell(cell).either)
        Eq[Either[DecodingFailure, A]].eqv(xRes, yRes)
      }
      // try to prove the result with the current seed
      val result =
        Test.check(Test.Parameters.default.withInitialSeed(seed), test).passed
      result
    }
  }
}
