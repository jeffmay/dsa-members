package zio
package csv.interop

import csv.interop.catz._
import csv.scalacheck.instances._
import csv.{Cell, CellDecoder, CellDecodingFailure}

import cats.laws.discipline.{MonadErrorTests, SemigroupKTests}
import munit.DisciplineSuite
import org.scalacheck.Prop

class catzSpec extends DisciplineSuite with ImplicitScalaCheckSeedForSuite {

  // TODO: Should this be a test helper somewhere?
  // Maybe there is some ZIO transform I can use?
  private val runtime: Runtime[Any] = Runtime.default

  private def evalEither[E, A](zio: IO[E, A]): Either[E, A] =
    evalOrThrow(zio.either)

  // TODO: Require E <:< Throwable
  private def evalOrThrow[E, A](zio: IO[E, A]): A =
    Unsafe.unsafe(implicit unsafe => runtime.unsafe.run(zio).getOrThrowFiberFailure())

  checkAll(
    "CellDecoder[Int]",
    MonadErrorTests[CellDecoder, CellDecodingFailure].monadError[Int, Int, Int],
  )

  checkAll(
    "CellDecoder[Int]",
    SemigroupKTests[CellDecoder].semigroupK[Int],
  )

  private[this] def transformations[T : Tag] =
    Map[String, CellDecoder[T] => CellDecoder[T]](
      "prepare" -> (_.prepare(identity)),
      "map" -> (_.map(identity)),
      "mapSafe" -> (_.mapSafe(identity)),
      "emap" -> (_.emap(Right(_))),
    )

  private[this] def containerDecoders[T : CellDecoder] = Seq[CellDecoder[_]](
    CellDecoder.split(',').as[T].combineFailures.to(Seq),
    CellDecoder.split("\\.".r).as[T].combineFailures.to(Seq),
  )

  for ((opName, transformation) <- transformations[Int]) {

    property(
      s"CellDecoder[Int].$opName should do nothing when used with identity",
    ) {
      val decoder = transformation(CellDecoder[Int])
      Prop.forAll { (i: Int) =>
        val actual = evalOrThrow {
          decoder.decodeCell(Cell.detached(i.toString)).either
        }
        assertEquals(actual, Right(i))
      }
    }

    property(
      s"CellDecoder[Int].$opName should fail when called on failed decoder",
    ) {
      val reason = "Test failure message"
      val decoder =
        transformation(CellDecoder.failWithMessage[Int](reason))
      Prop.forAll { (i: Int) =>
        val cell = Cell.detached(i.toString)
        val actual = evalEither {
          decoder.decodeString(i.toString).provideEnvironment(cell.toEnv)
        }
        val expected = evalOrThrow {
          CellDecodingFailure.fromMessage(reason).provideEnvironment(cell.toEnv)
        }
        assertEquals(actual, Left(expected))
      }
    }
  }

//  "transformations" should "not break derived decoders when called on Decoder[Option[T]]" in
//    forAll(transformations[Option[String]]) { transformation =>
//      implicit val decodeOptionString: Decoder[Option[String]] =
//        transformation(Decoder.decodeOption(Decoder.decodeString))
//
//      object Test {
//        implicit val eqTest: Eq[Test] = Eq.fromUniversalEquals
//        implicit val decodeTest: Decoder[Test] = Decoder.forProduct1("a")(Test.apply)
//      }
//
//      case class Test(a: Option[String])
//      // Dotty crashes here with `CyclicReference` on `assert`.
//      assertResult(Decoder[Test].decodeJson(Json.obj()))(Right(Test(None)))
//      assertResult(Decoder[Test].decodeAccumulating(Json.obj().hcursor))(Validated.valid(Test(None)))
//    }

  property(
    "CellDecoder[String].prepare should appropriately prepend a string",
  ) {
    Prop.forAll { (prefix: String, content: String) =>
      val cell = Cell.detached(content)
      val decoder = CellDecoder[String].prepare(prefix + _)
      val actual = evalEither {
        decoder.decodeString(content).provideEnvironment(cell.toEnv)
      }
      val expected = prefix + content
      assertEquals(actual, Right(expected))
    }
  }

//  "at" should "move appropriately" in forAll { (i: Int, k: String, m: Map[String, Int]) =>
//    assert(Decoder[Int].at(k).decodeJson(m.updated(k, i).asJson) === Right(i))
//  }
//
//  it should "accumulate errors" in forAll { (k: String, x: Boolean, xs: List[Boolean], m: Map[String, Int]) =>
//    val json = m.mapValues(_.asJson).toMap.updated(k, (x :: xs).asJson).asJson
//
//    assert(Decoder[List[Int]].at(k).decodeAccumulating(json.hcursor).leftMap(_.size) === Validated.invalid(xs.size + 1))
//  }

  property("CellDecoder[Int].emap should appropriately transform the result with an operation that can't fail") {
    Prop.forAll { (i: Int) =>
      val decoder = CellDecoder[Int].emap(v => Right(v + 1))
      val actual = evalEither {
        decoder.decodeCell(Cell.detached(i.toString))
      }
      assertEquals(actual, Right(i + 1))
    }
  }

  property("CellDecoder[Int].emap should appropriately transform the result with an operation that may fail") {
    Prop.forAll {
      (i: Int) =>
        val cell = Cell.detached(i.toString)
        val decoder = CellDecoder[Int].emap { v =>
          if (v % 2 == 0) Right(v) else Left("Odd")
        }
        val expected = evalEither {
          if (i % 2 == 0) ZIO.succeed(i)
          else CellDecodingFailure.fromMessage("Odd").flip.provideEnvironment(
            cell.toEnv,
          )
        }
        val actual = evalEither {
          decoder.decodeString(i.toString).provideEnvironment(cell.toEnv)
        }
        assertEquals(actual, expected)
    }
  }

//  "emapTry" should "appropriately transform the result with an operation that can't fail" in forAll { (i: Int) =>
//    assert(Decoder[Int].emapTry(v => Success(v + 1)).decodeJson(i.asJson) === Right(i + 1))
//  }
//
//  it should "appropriately transform the result with an operation that may fail" in forAll { (i: Int) =>
//    val exception = new Exception("Odd") with NoStackTrace
//    val decoder = Decoder[Int].emapTry(v => if (v % 2 == 0) Success(v) else Failure(exception))
//
//    assert(decoder.decodeJson(i.asJson).isRight == (i % 2 == 0))
//  }

  /*

  "handleErrorWith" should "respect the underlying decoder's tryDecode (#1271)" in {
    val decoder: Decoder[Option[String]] =
      Decoder.decodeOption[String].handleErrorWith(_ => Decoder.const(None)).at("a")

    assert(decoder.decodeJson(Json.obj("a" := 1)) === Right(None))
    assert(decoder.decodeJson(Json.obj("a" := Json.Null)) === Right(None))
    assert(decoder.decodeJson(Json.obj("b" := "abc")) === Right(None))
    assert(decoder.decodeJson(Json.obj("a" := "abc")) === Right(Some("abc")))

    assert(decoder.decodeAccumulating(Json.obj("a" := 1).hcursor) === Validated.valid(None))
    assert(decoder.decodeAccumulating(Json.obj("a" := Json.Null).hcursor) === Validated.valid(None))
    assert(decoder.decodeAccumulating(Json.obj("b" := "abc").hcursor) === Validated.valid(None))
    assert(decoder.decodeAccumulating(Json.obj("a" := "abc").hcursor) === Validated.valid(Some("abc")))
  }

  "failedWithMessage" should "replace the message" in forAll { (json: Json) =>
    assert(Decoder.failedWithMessage[Int]("Bad").decodeJson(json) === Left(DecodingFailure("Bad", Nil)))
  }

  "An optional object field decoder" should "fail appropriately" in {
    val decoder: Decoder[Option[String]] = Decoder.instance(
      _.downField("").downField("").as[Option[String]]
    )

    forAll { (json: Json) =>
      val result = decoder.apply(json.hcursor)

      assert(
        json.asObject match {
          // The top-level value isn't an object, so we should fail.
          case None => result.isLeft
          case Some(o1) =>
            o1("") match {
              // The top-level object doesn't contain a "" key, so we should succeed emptily.
              case None => result === Right(None)
              case Some(j2) =>
                j2.asObject match {
                  // The second-level value isn't an object, so we should fail.
                  case None => result.isLeft
                  case Some(o2) =>
                    o2("") match {
                      // The second-level object doesn't contain a "" key, so we should succeed emptily.
                      case None => result === Right(None)
                      // The third-level value is null, so we succeed emptily.
                      case Some(j3) if j3.isNull => result === Right(None)
                      case Some(j3) =>
                        j3.asString match {
                          // The third-level value isn't a string, so we should fail.
                          case None => result.isLeft
                          // The third-level value is a string, so we should have decoded it.
                          case Some(s3) => result === Right(Some(s3))
                        }
                    }
                }
            }
        }
      )
    }
  }

  "An optional array position decoder" should "fail appropriately" in {
    val decoder: Decoder[Option[String]] = Decoder.instance(
      _.downN(0).downN(1).as[Option[String]]
    )

    forAll { (json: Json) =>
      val result = decoder.apply(json.hcursor)

      assert(
        json.asArray match {
          // The top-level value isn't an array, so we should fail.
          case None => result.isLeft
          case Some(a1) =>
            a1.lift(0) match {
              // The top-level array is empty, so we should succeed emptily.
              case None => result === Right(None)
              case Some(j2) =>
                j2.asArray match {
                  // The second-level value isn't an array, so we should fail.
                  case None => result.isLeft
                  case Some(a2) =>
                    a2.lift(1) match {
                      // The second-level array doesn't have a second element, so we should succeed emptily.
                      case None => result === Right(None)
                      // The third-level value is null, so we succeed emptily.
                      case Some(j3) if j3.isNull => result === Right(None)
                      case Some(j3) =>
                        j3.asString match {
                          // The third-level value isn't a string, so we should fail.
                          case None => result.isLeft
                          // The third-level value is a string, so we should have decoded it.
                          case Some(s3) => result === Right(Some(s3))
                        }
                    }
                }
            }
        }
      )
    }
  }

  "An optional top-level decoder" should "fail appropriately" in {
    val decoder: Decoder[Option[String]] = Decoder.instance(_.as[Option[String]])

    forAll { (json: Json) =>
      val result = decoder.apply(json.hcursor)

      assert(
        if (json.isNull) {
          result === Right(None)
        } else
          json.asString match {
            case Some(str) => result === Right(Some(str))
            case None      => result.isLeft
          }
      )
    }
  }

  "A nested optional decoder" should "accumulate failures" in {
    val pair = Json.arr(Json.fromInt(1), Json.fromInt(2))

    val result = Decoder[Option[(String, String)]].decodeAccumulating(pair.hcursor)
    val expected = Validated.invalid(
      NonEmptyList.of(DecodingFailure("String", List(DownN(0))), DecodingFailure("String", List(DownN(1))))
    )

    assert(result === expected)
  }

  "instanceTry" should "provide instances that succeed or fail appropriately" in forAll { (json: Json) =>
    val exception = new Exception("Not an Int")
    val expected = json.hcursor.as[Int].leftMap(_ => DecodingFailure.fromThrowable(exception, Nil))
    val instance = Decoder.instanceTry(c => Try(c.as[Int].getOrElse(throw exception)))

    assert(instance.decodeJson(json) === expected)
  }

  "Decoder[Byte]" should "fail on out-of-range values (#83)" in forAll { (l: Long) =>
    val json = Json.fromLong(l)
    val result = Decoder[Byte].apply(json.hcursor)

    assert(if (l.toByte.toLong == l) result === Right(l.toByte) else result.isLeft)
  }

  it should "fail on non-whole values (#83)" in forAll { (d: Double) =>
    val json = Json.fromDoubleOrNull(d)
    val result = Decoder[Byte].apply(json.hcursor)

    assert(d.isWhole || result.isLeft)
  }

  it should "succeed on whole decimal values (#83)" in forAll { (v: Byte, n: Byte) =>
    val zeros = "0" * (math.abs(n.toInt) + 1)
    val Right(json) = parse(s"$v.$zeros")

    assert(Decoder[Byte].apply(json.hcursor) === Right(v))
  }

  "Decoder[Short]" should "fail on out-of-range values (#83)" in forAll { (l: Long) =>
    val json = Json.fromLong(l)
    val result = Decoder[Short].apply(json.hcursor)

    assert(if (l.toShort.toLong == l) result === Right(l.toShort) else result.isLeft)
  }

  it should "fail on non-whole values (#83)" in forAll { (d: Double) =>
    val json = Json.fromDoubleOrNull(d)
    val result = Decoder[Short].apply(json.hcursor)

    assert(d.isWhole || result.isLeft)
  }

  it should "succeed on whole decimal values (#83)" in forAll { (v: Short, n: Byte) =>
    val zeros = "0" * (math.abs(n.toInt) + 1)
    val Right(json) = parse(s"$v.$zeros")

    assert(Decoder[Short].apply(json.hcursor) === Right(v))
  }

  "Decoder[Int]" should "fail on out-of-range values (#83)" in forAll { (l: Long) =>
    val json = Json.fromLong(l)
    val result = Decoder[Int].apply(json.hcursor)

    assert(if (l.toInt.toLong == l) result === Right(l.toInt) else result.isLeft)
  }

  it should "fail on non-whole values (#83)" in forAll { (d: Double) =>
    val json = Json.fromDoubleOrNull(d)
    val result = Decoder[Int].apply(json.hcursor)

    assert(d.isWhole || result.isLeft)
  }

  it should "succeed on whole decimal values (#83)" in forAll { (v: Int, n: Byte) =>
    val zeros = "0" * (math.abs(n.toInt) + 1)
    val Right(json) = parse(s"$v.$zeros")

    assert(Decoder[Int].apply(json.hcursor) === Right(v))
  }

  "Decoder[Long]" should "fail on out-of-range values (#83)" in forAll { (i: BigInt) =>
    val json = Json.fromBigDecimal(BigDecimal(i))
    val result = Decoder[Long].apply(json.hcursor)

    assert(if (BigInt(i.toLong) == i) result === Right(i.toLong) else result.isLeft)
  }

  it should "fail on non-whole values (#83)" in forAll { (d: Double) =>
    val json = Json.fromDoubleOrNull(d)
    val result = Decoder[Long].apply(json.hcursor)

    assert(d.isWhole || result.isLeft)
  }

  "Decoder[Float]" should "attempt to parse string values as doubles (#173)" in forAll { (d: Float) =>
    val Right(json) = parse("\"" + d.toString + "\"")

    assert(Decoder[Float].apply(json.hcursor) === Right(d))
  }

  it should "match the rounding of Float.parseFloat (#1063)" in forAll { (d: Double) =>
    val Right(json) = parse(d.toString)

    assert(Decoder[Float].apply(json.hcursor) === Right(java.lang.Float.parseFloat(d.toString)))
  }

  it should "match the rounding of Float.parseFloat for known problematic inputs (#1063)" in {
    val bad1 = "1.199999988079071"
    val bad2 = "7.038531E-26"

    val Right(json1) = parse(bad1)
    val Right(json2) = parse(bad2)

    assert(Decoder[Float].apply(json1.hcursor) === Right(java.lang.Float.parseFloat(bad1)))
    assert(Decoder[Float].apply(json2.hcursor) === Right(java.lang.Float.parseFloat(bad2)))
  }

  "Decoder[Double]" should "attempt to parse string values as doubles (#173)" in forAll { (d: Double) =>
    val Right(json) = parse("\"" + d.toString + "\"")

    assert(Decoder[Double].apply(json.hcursor) === Right(d))
  }

  "Decoder[BigInt]" should "fail when producing a value would be intractable" in {
    val Right(bigNumber) = parse("1e2147483647")

    assert(Decoder[BigInt].apply(bigNumber.hcursor).isLeft)
  }

  val isPositive: Int => Boolean = _ > 0
  val isOdd: Int => Boolean = _ % 2 != 0

  "ensure" should "fail appropriately on an invalid result" in forAll { (i: Int) =>
    val message = "Not positive!"

    val decodePositiveInt: Decoder[Int] = Decoder[Int].ensure(_ > 0, message)
    val expected = if (i > 0) Right(i) else Left(DecodingFailure(message, Nil))

    assert(decodePositiveInt.decodeJson(Json.fromInt(i)) === expected)
  }

  it should "only include the first failure when chained, even in error-accumulation mode" in forAll { (i: Int) =>
    val positiveMessage = "Not positive!"
    val oddMessage = "Not odd!"

    val badDecodePositiveOddInt: Decoder[Int] =
      Decoder[Int].ensure(isPositive, positiveMessage).ensure(isOdd, oddMessage)

    val expected = if (isPositive(i)) {
      if (isOdd(i)) {
        Validated.valid(i)
      } else {
        Validated.invalidNel(DecodingFailure(oddMessage, Nil))
      }
    } else {
      Validated.invalidNel(DecodingFailure(positiveMessage, Nil))
    }

    assert(badDecodePositiveOddInt.decodeAccumulating(Json.fromInt(i).hcursor) === expected)
  }

  it should "not include failures it hasn't checked for" in {
    val decodePositiveInt: Decoder[Int] =
      Decoder[Int].ensure(isPositive, "Not positive!")

    val expected = Validated.invalidNel(DecodingFailure("Int", Nil))

    assert(decodePositiveInt.decodeAccumulating(Json.Null.hcursor) === expected)
  }

  it should "include all given failures in error-accumulation mode" in forAll { (i: Int) =>
    val positiveMessage = "Not positive!"
    val oddMessage = "Not odd!"

    val decodePositiveOddInt: Decoder[Int] =
      Decoder[Int].ensure(i =>
        (if (isPositive(i)) Nil else List(positiveMessage)) ++
          (if (isOdd(i)) Nil else List(oddMessage))
      )

    val expected = if (isPositive(i)) {
      if (isOdd(i)) {
        Validated.valid(i)
      } else {
        Validated.invalidNel(DecodingFailure(oddMessage, Nil))
      }
    } else {
      if (isOdd(i)) {
        Validated.invalidNel(DecodingFailure(positiveMessage, Nil))
      } else {
        Validated.invalid(NonEmptyList.of(DecodingFailure(positiveMessage, Nil), DecodingFailure(oddMessage, Nil)))
      }
    }

    assert(decodePositiveOddInt.decodeAccumulating(Json.fromInt(i).hcursor) === expected)
  }

  "validate" should "fail appropriately on invalid input in fail-fast mode" in forAll { (i: Int) =>
    val message = "Not positive!"

    val decodePositiveInt: Decoder[Int] =
      Decoder[Int].validate(_.as[Int].exists(_ > 0), message)

    val expected = if (i > 0) Right(i) else Left(DecodingFailure(message, Nil))

    assert(decodePositiveInt.decodeJson(Json.fromInt(i)) === expected)
  }

  it should "fail appropriately on invalid input in error-accumulation mode (#865)" in forAll { (i: Int) =>
    val message = "Not positive!"

    val decodePositiveInt: Decoder[Int] =
      Decoder[Int].validate(_.as[Int].exists(_ > 0), message)

    val expected = if (i > 0) Validated.valid(i) else Validated.invalidNel(DecodingFailure(message, Nil))

    assert(decodePositiveInt.decodeAccumulating(Json.fromInt(i).hcursor) === expected)
  }

  it should "not infinitely recurse (#396)" in forAll { (i: Int) =>
    assert(Decoder[Int].validate(_ => true, "whatever").apply(Json.fromInt(i).hcursor) === Right(i))
  }

  it should "preserve error accumulation when validation succeeds" in {
    val message = "This shouldn't work"

    trait Foo

    val decoder: Decoder[Foo] = new Decoder[Foo] {
      override def apply(c: HCursor): Decoder.Result[Foo] = Right(new Foo {})

      override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[Foo] = Invalid(
        NonEmptyList.one(DecodingFailure(message, c.history))
      )
    }

    val validatingDecoder = decoder.validate(c => true, "Foobar")

    assert(validatingDecoder.decodeAccumulating(Json.True.hcursor).isInvalid)
  }

  it should "provide the generated error messages from HCursor when a function is passed" in {
    case class Foo(x: Int, y: String)

    val decoder: Decoder[Foo] = Decoder.const(Foo(42, "meaning")).validate { c =>
      val maybeFieldsStr = for {
        json <- c.focus
        jsonKeys <- json.hcursor.keys
      } yield jsonKeys.mkString(",")
      maybeFieldsStr.getOrElse("") :: Nil
    }

    val Right(fooJson) = parse("""{"x":42, "y": "meaning"}""")

    assert(decoder.decodeJson(fooJson).swap.exists(_.message === "x,y"))
  }

  it should "not fail when the passed errors function returns an empty list" in {
    val testValue = 42
    val decoder = Decoder[Int].validate(_ => Nil)

    val Right(intJson) = parse(testValue.toString)

    assert(decoder.decodeJson(intJson) === Right(testValue))
  }

  "either" should "return the correct disjunct" in forAll { (value: Either[String, Boolean]) =>
    val json = value match {
      case Left(s)  => Json.fromString(s)
      case Right(b) => Json.fromBoolean(b)
    }

    assert(Decoder[String].either(Decoder[Boolean]).decodeJson(json) === Right(value))
  }

  it should "respect the underlying decoder's tryDecode (#1271)" in {
    val decoder: Decoder[Either[Option[String], Boolean]] =
      Decoder.decodeOption[String].either(Decoder.const(true)).at("a")

    assert(decoder.decodeJson(Json.obj("a" := 1)) === Right(Right(true)))
    assert(decoder.decodeJson(Json.obj("a" := Json.Null)) === Right(Left(None)))
    assert(decoder.decodeJson(Json.obj("b" := "abc")) === Right(Left(None)))
    assert(decoder.decodeJson(Json.obj("a" := "abc")) === Right(Left(Some("abc"))))

    assert(decoder.decodeAccumulating(Json.obj("a" := 1).hcursor) === Validated.valid(Right(true)))
    assert(decoder.decodeAccumulating(Json.obj("a" := Json.Null).hcursor) === Validated.valid(Left(None)))
    assert(decoder.decodeAccumulating(Json.obj("b" := "abc").hcursor) === Validated.valid(Left(None)))
    assert(decoder.decodeAccumulating(Json.obj("a" := "abc").hcursor) === Validated.valid(Left(Some("abc"))))
  }

  "or" should "respect the underlying decoder's tryDecode (#1271)" in {
    val decoder: Decoder[Option[String]] =
      Decoder.decodeOption[String].or(Decoder.const(Option.empty[String])).at("a")

    assert(decoder.decodeJson(Json.obj("a" := 1)) === Right(None))
    assert(decoder.decodeJson(Json.obj("a" := Json.Null)) === Right(None))
    assert(decoder.decodeJson(Json.obj("b" := "abc")) === Right(None))
    assert(decoder.decodeJson(Json.obj("a" := "abc")) === Right(Some("abc")))

    assert(decoder.decodeAccumulating(Json.obj("a" := 1).hcursor) === Validated.valid(None))
    assert(decoder.decodeAccumulating(Json.obj("a" := Json.Null).hcursor) === Validated.valid(None))
    assert(decoder.decodeAccumulating(Json.obj("b" := "abc").hcursor) === Validated.valid(None))
    assert(decoder.decodeAccumulating(Json.obj("a" := "abc").hcursor) === Validated.valid(Some("abc")))
  }

  private[this] val stateful = {
    import Decoder.state._
    Decoder.fromState(for {
      a <- decodeField[String]("a")
      b <- decodeField[String]("b")
      _ <- requireEmpty
    } yield a ++ b)
  }

  "a stateful Decoder with requireEmpty" should "succeed when there are no leftover fields" in {
    val json = Json.obj("a" -> "1".asJson, "b" -> "2".asJson)

    assert(stateful.decodeJson(json) === Right("12"))
  }

  it should "fail when there are leftover fields" in {
    val json = Json.obj("a" -> "1".asJson, "b" -> "2".asJson, "c" -> "3".asJson, "d" -> "4".asJson)

    assert(stateful.decodeJson(json).swap.exists(_.message === "Leftover keys: c, d"))
  }

  it should "fail normally when a field is missing" in {
    val json = Json.obj("a" -> "1".asJson)

    assert(stateful.decodeJson(json).swap.exists(_.message === "Attempt to decode value on failed cursor"))
  }

  private[this] val statefulOpt = {
    import Decoder.state._
    Decoder.fromState(for {
      a <- decodeField[Option[String]]("a")
      b <- decodeField[String]("b")
      _ <- requireEmpty
    } yield a.foldMap(identity) ++ b)
  }

  "a stateful Decoder with requireEmpty and an optional value" should
    "succeed when there are no leftover fields and an optional field is missing" in {
      val json = Json.obj("b" -> "2".asJson)

      assert(statefulOpt.decodeJson(json) === Right("2"))
    }

  it should "succeed when there are no leftover fields and an optional field is present" in {
    val json = Json.obj("a" -> "1".asJson, "b" -> "2".asJson)

    assert(statefulOpt.decodeJson(json) === Right("12"))
  }

  it should "fail when there are leftover fields and an optional field is missing" in {
    val json = Json.obj("b" -> "2".asJson, "c" -> "3".asJson, "d" -> "4".asJson)

    assert(statefulOpt.decodeJson(json).swap.exists(_.message === "Leftover keys: c, d"))
  }

  it should "fail when there are leftover fields and an optional field is present" in {
    val json = Json.obj("a" -> "1".asJson, "b" -> "2".asJson, "c" -> "3".asJson, "d" -> "4".asJson)

    assert(statefulOpt.decodeJson(json).swap.exists(_.message === "Leftover keys: c, d"))
  }

  it should "fail normally when a field is missing and an optional field is present" in {
    val json = Json.obj("a" -> "1".asJson)

    assert(statefulOpt.decodeJson(json).swap.exists(_.message === "Attempt to decode value on failed cursor"))
  }

  it should "fail normally when a field is missing and an optional field is missing" in {
    val json = Json.obj()

    assert(statefulOpt.decodeJson(json).swap.exists(_.message === "Attempt to decode value on failed cursor"))
  }

  checkAll("Codec[WrappedOptionalField]", CodecTests[WrappedOptionalField].codec)

  "decodeSet" should "match sequence decoders" in forAll { (xs: List[Int]) =>
    assert(Decoder.decodeSet[Int].decodeJson(xs.asJson) === Decoder[Seq[Int]].map(_.toSet).decodeJson(xs.asJson))
  }

  "decodeList" should "match sequence decoders" in forAll { (xs: List[Int]) =>
    assert(Decoder.decodeList[Int].decodeJson(xs.asJson) === Decoder[Seq[Int]].map(_.toList).decodeJson(xs.asJson))
  }

  "decodeVector" should "match sequence decoders" in forAll { (xs: List[Int]) =>
    assert(Decoder.decodeVector[Int].decodeJson(xs.asJson) === Decoder[Seq[Int]].map(_.toVector).decodeJson(xs.asJson))
  }

  "decodeChain" should "match sequence decoders" in forAll { (xs: List[Int]) =>
    assert(
      Decoder.decodeChain[Int].decodeJson(xs.asJson) === Decoder[Seq[Int]].map(Chain.fromSeq(_)).decodeJson(xs.asJson)
    )
  }

  "HCursor#history" should "be stack safe" in {
    val size = 10000
    val json = List.fill(size)(1).asJson.mapArray(_ :+ true.asJson)
    val Left(DecodingFailure(_, history)) = Decoder[List[Int]].decodeJson(json)

    assert(history.size === size + 1)
  }

  case class NotDecodable(a: Int)
  implicit val decodeNotDecodable: Decoder[NotDecodable] = Decoder.failedWithMessage("Some message")

  "container decoder" should "pass through error message from item" in forAll(containerDecoders[NotDecodable]) {
    decoder =>
      val json = Json.arr(Json.obj("a" -> 1.asJson))
      assert(decoder.decodeJson(json) == Left(DecodingFailure("Some message", List(DownArray))))
  }

  "decodeAccumulating" should "accumulate errors after traversal" in {
    import cats.syntax.functor._
    import cats.syntax.traverse._

    val decoder: Decoder[Unit] = List(
      Decoder[Int].ensure(_ > 0, "pos"),
      Decoder[Int].ensure(_ % 2 != 0, "odd")
    ).sequence.void

    val result = decoder.decodeAccumulating(Json.fromInt(-2).hcursor)

    assert(result.isInvalid)
    assert(result.swap.toOption.map(_.size) === Some(2))
  }
   */
}
