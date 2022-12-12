package zio.util

import zio.test.*
import zio.test.Assertion.*
import scala.IArray

object SubTypeOrdinalSpec extends ZIOSpecDefault {
  override def spec: Spec[Any, Any] = suite("SubTypeOrdinal")(
    test(".ordinal works") {
      def isFirst[E : SubTypeOrdinal](enumCase: E): TestResult = assertTrue(enumCase.ordinal == 0)
      isFirst(OrdinalExample.One)
    },
    test(".ordinal fails to compile") {
      assertZIO(typeCheck("summon[Ordinal[OrdinalCounterExample]]"))(isLeft)
    },
  )
}

enum OrdinalExample derives SubTypeOrdinal:
  case One, Two

final case class OrdinalCounterExample(one: Int)
