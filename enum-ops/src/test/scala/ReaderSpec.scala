package zio.util

import zio.test.*

object ReaderSpec extends ZIOSpecDefault {
  override def spec: Spec[Any, Any] = suite("ExampleReader")(
    test("works") {
      val reader = summon[ExampleReader[ReaderExample]]
      assertTrue(reader.readString("one") == ReaderExample.One)
    },
  )
}

enum ReaderExample:
  case One, Two

object ReaderExample extends EnumCompanionOf[ReaderExample]:
  import DiscriminatorFormat.lowerCase
  given reader: ExampleReader[ReaderExample] = ExampleReader.enumOf(this)
