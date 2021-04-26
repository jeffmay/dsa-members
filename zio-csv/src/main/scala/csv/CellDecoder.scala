package zio
package csv

import enumeratum.ops.EnumCodec
import zio.stream.{ZSink, ZStream}

import java.time.{Instant, LocalDate, LocalDateTime, ZonedDateTime}
import scala.collection.Factory
import scala.collection.immutable.ArraySeq
import scala.util.Try
import scala.util.matching.Regex

trait CellDecoder[A] {

  /** Decode the given cell contents into the expected type or return a [[CellDecodingFailure]].
    */
  def decodeString(content: String): ZIO[
    CellDecoder.MinCtx,
    CellDecodingFailure,
    A,
  ]

  /** Decode the given [[Cell]] into the expected type or return a [[CellDecodingFailure]].
    */
  final def decodeCell(cell: Cell): IO[DecodingFailure, A] = {
    for {
      env <- cell.asEnv
      res <- decodeString(env.get[CellCtx].content).provide(env)
    } yield res
  }

  /** Apply a function to the decoded result to get a new decoder, use it to decode the cell, and don't
    * catch any exceptions thrown while building the decoder.
    */
  final def flatMap[B](fn: A => CellDecoder[B]): CellDecoder[B] = { content =>
    decodeString(content).flatMap { a =>
      val decodeB = fn(a)
      decodeB.decodeString(content)
    }
  }

  /** Apply a function to the decoded result to get a new decoder, use it to decode the cell, and wrap any
    * caught exceptions from building the decoder in a [[CellDecodingException]] of the final expected type.
    */
  final def flatMapSafe[B : Tag](fn: A => CellDecoder[B]): CellDecoder[B] = {
    content =>
      decodeString(content).flatMap { a =>
        ZIO.fromTry(Try(fn(a))).flatMap { decodeB =>
          decodeB.decodeString(content)
        }.flatMapError { ex =>
          CellDecodingFailure.buildFromContextValues {
            CellDecodingException[B](_, _, _, ex)
          }
        }
      }
  }

  /** If this decoder fails, fall back to the given decoder and return either the successful result
    * of this decoder or the final result of the given decoder.
    */
  final def or[B >: A](decoder: => CellDecoder[B]): CellDecoder[B] =
    CellDecoder.fromEffect { content =>
      decodeString(content).orElse(decoder.decodeString(content))
    }

  /** Prepare the content of the cell before applying this decoder.
    */
  final def prepare(fn: String => String): CellDecoder[A] = { content =>
    decodeString(fn(content))
  }

  /** Apply a function to the decoded result and don't catch any exceptions.
    */
  final def map[B](fn: A => B): CellDecoder[B] = { content =>
    decodeString(content).map(fn)
  }

  /** Apply a function to the decoded result that can return a failure synchronously.
    */
  final def emap[B](fn: A => Either[String, B]): CellDecoder[B] = { content =>
    decodeString(content).flatMap { a =>
      ZIO.fromEither(fn(a)).flatMapError { reason =>
        CellDecodingFailure.fromMessage(reason)
      }
    }
  }

  /** Apply a function to the decoded result and wrap any exceptions in a [[CellDecodingException]].
    */
  final def mapSafe[B : Tag](fn: A => B): CellDecoder[B] = { content =>
    decodeString(content).flatMap { a =>
      ZIO.fromTry(Try(fn(a))).flatMapError { ex =>
        CellDecodingFailure.buildFromContextValues {
          CellDecodingException[B](_, _, _, ex)
        }
      }
    }
  }

  /** Run two decoders and return their results as a pair.
    */
  final def product[B](fb: CellDecoder[B]): CellDecoder[(A, B)] = { c =>
    flatMap(a => fb.map(b => (a, b))).decodeString(c)
  }
}

object CellDecoder {
  type MinCtx = Has[RowCtx] with Has[CellCtx] with Has[MaybeHeaderCtx]
  type Result[A] = ZIO[MinCtx, CellDecodingFailure, A]

  @inline def apply[A](implicit
    decoder: CellDecoder[A],
  ): CellDecoder[A] = decoder

  @inline def split(delimiter: Char): SplitString =
    split(delimiter, keepEmpty = true)

  def split(delimiter: Char, keepEmpty: Boolean): SplitString = {
    val adjust: Seq[String] => Seq[String] =
      if (keepEmpty) identity else _.filterNot(_.isEmpty)
    new SplitString(s => adjust(ArraySeq.unsafeWrapArray(s.split(delimiter))))
  }

  @inline def split(re: Regex): SplitString = split(re, keepEmpty = true)

  def split(re: Regex, keepEmpty: Boolean): SplitString = {
    val adjust: Seq[String] => Seq[String] =
      if (keepEmpty) identity else _.filterNot(_.isEmpty)
    new SplitString(s => adjust(ArraySeq.unsafeWrapArray(re.split(s))))
  }

  trait Split[A] extends Any {

    protected def split: String => ZStream[MinCtx, CellDecodingFailure, A]

    def to[C](factory: Factory[A, C]): CellDecoder[C] = {
      content => split(content).run(ZSink.collectAll).map(_.to(factory))
    }
  }

  final class SplitString(private val splitString: String => Seq[String])
    extends Split[String] {

    override protected val split: String => ZStream[
      MinCtx,
      CellDecodingFailure,
      String,
    ] = {
      // this is safe because the array is never mutated in this local scope
      // and all other references are to an immutable interface that copies
      // to a new array before allowing mutation.
      val decoder =
        CellDecoder.fromStringSafe(s => splitString(s))

      // create a function from the string content into a stream of values from the iterable
      content => ZStream.fromIterableM(decoder.decodeString(content))
    }

    def as[A : CellDecoder]: SplitMap[A] = new SplitMap[A](content => {
      split(content).mapM { piece =>
        CellDecoder[A].decodeString(piece)
      }
    })
  }

  final class SplitMap[A](
    override protected val split: String => ZStream[
      MinCtx,
      CellDecodingFailure,
      A,
    ],
  ) extends AnyVal
    with Split[A] {

    def droppingErrors: SplitMap[A] =
      new SplitMap[A](split.andThen(_.catchAll(_ => ZStream.empty)))
  }

  implicit val string: CellDecoder[String] = ZIO.succeed(_)

  implicit val boolean: CellDecoder[Boolean] = {
    val validTrue = Set("true", "yes", "y", "on", "1")
    val validFalse = Set("false", "no", "n", "off", "0")
    val validOptions =
      (validTrue.toSeq ++ validFalse.toSeq).mkString("'", "', '", "'")
    fromStringSafe { raw =>
      // skip obviously wrong values
      if (raw.length > "false".length) false
      else {
        val lowercase = raw.trim.toLowerCase
        if (validTrue.contains(lowercase)) true
        else if (validFalse.contains(lowercase)) false
        else throw new IllegalArgumentException(
          s"Unknown value '$raw'. Expected one of $validOptions",
        )
      }
    }
  }

  implicit val int: CellDecoder[Int] = fromStringSafe(_.toInt)
  implicit val long: CellDecoder[Long] = fromStringSafe(_.toLong)
  implicit val bigInt: CellDecoder[BigInt] = fromStringSafe(BigInt(_))
  implicit val float: CellDecoder[Float] = fromStringSafe(_.toFloat)
  implicit val double: CellDecoder[Double] = fromStringSafe(_.toDouble)
  implicit val bigDecimal: CellDecoder[BigDecimal] =
    fromStringSafe(BigDecimal(_))

  // TODO: Come up with more tolerant format options
  implicit val instant: CellDecoder[Instant] = fromStringSafe(Instant.parse(_))
  implicit val localDate: CellDecoder[LocalDate] =
    fromStringSafe(LocalDate.parse(_))
  implicit val localDateTime: CellDecoder[LocalDateTime] =
    fromStringSafe(LocalDateTime.parse(_))
  implicit val zonedDateTime: CellDecoder[ZonedDateTime] =
    fromStringSafe(ZonedDateTime.parse(_))

  implicit def optional[A : CellDecoder]: CellDecoder[Option[A]] =
    fromEffect { str =>
      val trimmed = str.trim
      if (trimmed.isEmpty) {
        ZIO.succeed(None)
      } else {
        CellDecoder[A].decodeString(trimmed).map(Option(_))
      }
    }

  /** Does a match on the enum values based on the [[EnumCodec]]
    */
  implicit def fromEnum[E : EnumCodec : Tag]: CellDecoder[E] = { cell =>
    ZIO.fromEither {
      EnumCodec[E].findByNameInsensitiveEither(cell)
    }.flatMapError {
      CellDecodingFailure.fromExceptionDecodingAs[E](_)
    }
  }

  def const[A](value: A): CellDecoder[A] = new CellDecoder[A] {
    final override def decodeString(
      content: String,
    ): ZIO[MinCtx, CellDecodingFailure, A] = ZIO.succeed(value)
    final override def toString: String = s"CellDecoder.const($value)"
  }

  def fail[A](failure: CellDecodingFailure): CellDecoder[A] =
    failWith(_ => ZIO.succeed(failure))

  def failWith[A](failWith: String => URIO[
    CellDecoder.MinCtx,
    CellDecodingFailure,
  ]): CellDecoder[A] =
    content => {
      for {
        failure <- failWith(content)
        failed <- ZIO.fail(failure)
      } yield failed
    }

  def failWithMessage[A](reason: String): CellDecoder[A] =
    failWith[A] { _ =>
      CellDecodingFailure.fromMessage(reason)
    }

  def fromStringSafe[A : Tag](convert: String => A): CellDecoder[A] = { str =>
    ZIO(convert(str))
      .flatMapError { ex =>
        CellDecodingFailure.buildFromContextValues {
          CellDecodingException[A](_, _, _, ex)
        }
      }
  }

  def fromStringTotal[A](convert: String => A): CellDecoder[A] =
    str => ZIO.succeed(convert(str))

  def fromEffect[A](convert: String => CellDecoder.Result[A]): CellDecoder[A] =
    convert(_)

  def fromEither[A](
    convert: String => Either[CellDecodingFailure, A],
  ): CellDecoder[A] = { str =>
    ZIO.fromEither(convert(str))
  }

  def matchesRegex(re: Regex): CellDecoder[String] = fromEffect { str =>
    ZIO.fromOption(Option.when(re.matches(str))(str))
      .flatMapError { _ =>
        CellDecodingFailure.buildFromContextValues {
          CellInvalidUnmatchedRegex[String](_, _, _, re)
        }
      }
  }

  def findAllMatches(re: Regex): CellDecoder[Iterable[Regex.Match]] =
    fromEffect { str =>
      val ll = LazyList.from(re.findAllMatchIn(str))
      ZIO.fromOption(Option.unless(ll.isEmpty)(ll))
        .flatMapError { _ =>
          CellDecodingFailure.buildFromContextValues {
            CellInvalidUnmatchedRegex[Iterable[Regex.Match]](_, _, _, re)
          }
        }
    }
}
