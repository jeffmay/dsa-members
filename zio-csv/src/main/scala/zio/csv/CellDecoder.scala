package zio.csv

import enumeratum.ops.{EnumCodec, IsEnum}
import zio.{Has, Tag, ZIO}

import java.time.{Instant, LocalDate, LocalDateTime, ZonedDateTime}
import scala.util.Try
import scala.util.matching.Regex

trait CellDecoder[A] {

  def decodeString(cell: String): CellDecoder.Result[A]

  def mapSafe[B : Tag](fn: A ⇒ B): CellDecoder[B] =
    flatMapSafe(a ⇒ CellDecoder.const(fn(a)))

  def flatMapSafe[B : Tag](fn: A ⇒ CellDecoder[B]): CellDecoder[B] =
    CellDecoder.fromEffect { cell ⇒
      decodeString(cell).flatMap { a ⇒
        ZIO.fromTry(Try(fn(a))).flatMap { decodeB ⇒
          decodeB.decodeString(cell)
        }.flatMapError { ex ⇒
          for {
            row ← ZIO.service[RowCtx]
            cell ← ZIO.service[CellCtx]
          } yield CellDecodingException[B](
            row.rowIndex,
            cell.columnIndex,
            ex,
          )
        }
      }
    }
}

object CellDecoder {
  type MinCtx = Has[RowCtx] with Has[CellCtx]
  type Result[A] = ZIO[MinCtx, CellDecodingFailure, A]

  @inline def apply[A](implicit
    decoder: CellDecoder[A],
  ): CellDecoder[A] = decoder

  implicit val string: CellDecoder[String] = ZIO.succeed(_)

  implicit val boolean: CellDecoder[Boolean] = {
    val validTrue = Set("true", "yes", "y", "on", "1")
    val validFalse = Set("false", "no", "n", "off", "0")
    val validOptions =
      (validTrue.toSeq ++ validFalse.toSeq).mkString("'", "', '", "'")
    fromStringSafe { raw ⇒
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
    fromEffect { str ⇒
      val trimmed = str.trim
      if (trimmed.isEmpty) {
        ZIO.succeed(None)
      } else {
        CellDecoder[A].decodeString(trimmed).map(Option(_))
      }
    }

  /** Does a match on the enum values based on the [[EnumCodec]]
    */
  implicit def fromEnum[E : IsEnum : Tag]: CellDecoder[E] = { cell ⇒
    ZIO.fromEither {
      IsEnum[E].codec.findByNameInsensitiveEither(cell)
    }.flatMapError {
      CellDecodingFailure.fromExceptionDecodingAs[E](_)
    }
  }

  def fromStringSafe[A : Tag](convert: String ⇒ A): CellDecoder[A] = { str ⇒
    ZIO(convert(str))
      .flatMapError { ex ⇒
        ZIO.services[RowCtx, CellCtx].map { case (row, cell) ⇒
          CellDecodingException[A](
            row.rowIndex,
            cell.columnIndex,
            ex,
          )
        }
      }
  }

  def const[A](value: A): CellDecoder[A] = { _ ⇒
    ZIO.succeed(value)
  }

  def fromEffect[A](convert: String ⇒ CellDecoder.Result[A]): CellDecoder[A] =
    convert(_)

  def fromEither[A](
    convert: String ⇒ Either[CellDecodingFailure, A],
  ): CellDecoder[A] = { str ⇒
    ZIO.fromEither(convert(str))
  }

  def matchesRegex(re: Regex): CellDecoder[String] = fromEffect { str ⇒
    ZIO.fromOption(Option.when(re.matches(str))(str))
      .flatMapError { _ ⇒
        for {
          row ← ZIO.service[RowCtx]
          cell ← ZIO.service[CellCtx]
        } yield CellInvalidUnmatchedRegex[String](
          row.rowIndex,
          cell.columnIndex,
          re,
        )
      }
  }

  def findAllMatches(re: Regex): CellDecoder[Iterable[Regex.Match]] =
    fromEffect { str ⇒
      val ll = LazyList.from(re.findAllMatchIn(str))
      ZIO.fromOption(Option.unless(ll.isEmpty)(ll))
        .flatMapError { _ ⇒
          for {
            row ← ZIO.service[RowCtx]
            cell ← ZIO.service[CellCtx]
          } yield CellInvalidUnmatchedRegex[Iterable[Regex.Match]](
            row.rowIndex,
            cell.columnIndex,
            re,
          )
        }
    }
}
