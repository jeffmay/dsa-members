package org.dsasf.members
package jobs

import database.models.{EmailAddress, InputEmailAddress, PhoneNumber, PhoneNumberRegion}

import zio.ZIO
import zio.csv.{CellDecoder, CellDecodingFailure}

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.ChronoField
import java.time.{DateTimeException, LocalDate}
import scala.util.Try
import scala.util.control.NoStackTrace

trait CommonDecoders {

  // implicit def decodeUnknownEntryOr[
  //   E : EnumCodec,
  // ]: CellDecoder[
  //   UnknownEntryOr[E],
  // ] = CellDecoder.fromStringTotal { str =>
  //   EnumCodec[E].findByNameInsensitive(str)
  // }

  /** Decodes a single phone number using the default region from context. */
  given decodePhoneNumber(using defaultRegion: PhoneNumberRegion): CellDecoder[PhoneNumber] =
    CellDecoder.fromEffect { content =>
      PhoneNumber.parseAndValidate(content, defaultRegion).fold(
        ex => CellDecodingFailure.fromMessage(ex.getMessage).flip,
        ZIO.succeed(_),
      )
    }

  /** Decodes a sequence of phone numbers (discarding invalid ones) using the default region from context. */
  given decodeSeqPhoneNumber(using defaultRegion: PhoneNumberRegion): CellDecoder[Seq[PhoneNumber]] =
    CellDecoder.split(',').as[PhoneNumber].skipFailures.to(Vector)

  given decodeEmailAddress: CellDecoder[EmailAddress] =
    CellDecoder.fromStringSafe { content =>
      require(
        content.nonEmpty,
        s"EmailAddress cannot be empty.",
      )
      val firstAtSymbol = content.indexOf('@')
      require(
        firstAtSymbol >= 0,
        s"EmailAddress '$content' requires an '@' symbol. None found.",
      )
      require(
        firstAtSymbol > 0,
        s"EmailAddress '$content' requires a username before the '@' symbol.",
      )
      val username = content.take(firstAtSymbol)
      require(
        username.nonEmpty,
        s"EmailAddress '$content' username cannot be empty.",
      )
      val domain = content.drop(firstAtSymbol + 1)
      require(
        domain.nonEmpty,
        s"EmailAddress '$content' domain cannot be empty.",
      )
      require(
        !domain.contains('@'),
        s"EmailAddress domain '$domain' cannot contain an '@' symbol. First symbol found at index $firstAtSymbol.",
      )
      InputEmailAddress.parse(s"$username@$domain").fold(msg => throw new IllegalArgumentException(msg), identity)
    }

  given decodeLocalDate: CellDecoder[LocalDate] = {
    val dsaLocalDateTime = new DateTimeFormatterBuilder()
      .append(DateTimeFormatter.ISO_LOCAL_DATE)
      .optionalStart()
      .appendLiteral(' ')
      .appendValue(ChronoField.HOUR_OF_DAY)
      .appendLiteral(':')
      .appendValue(ChronoField.MINUTE_OF_HOUR)
      .toFormatter

    // Use laziness in downstream operations, but start with the same cached starting collection
    val formats = LazyList(
      "DSA_LOCAL_DATE_TIME (YYYY-MM-DD[' 'HH:MM])" -> dsaLocalDateTime,
      "BASIC_ISO_DATE (YYYYMMDD)" -> DateTimeFormatter.BASIC_ISO_DATE,
      "RFC-1123 (DOW, DD Mon YYYY)" -> DateTimeFormatter.RFC_1123_DATE_TIME,
      "ISO_ORDINAL_DATE (YYYY-DOY)" -> DateTimeFormatter.ISO_ORDINAL_DATE,
    )
    CellDecoder.fromStringSafe { str =>
      // create two lazy lists: one that accumulate failures and one that accumulates successes
      val (failures, successes) =
        formats.partitionMap {
          case (name, format) =>
            Try(LocalDate.parse(str, format)).toEither.left.map {
              case ex: DateTimeException => (name, ex)
              case ex => throw ex
            }
        }
      // lazily find the first success, or fail with all the accumulated DateFormatExceptions
      successes.headOption.getOrElse {
        throw DateFormatException(str, failures)
      }
    }
  }
}

final case class DateFormatException(
  content: String,
  patternErrors: Seq[(String, DateTimeException)],
) extends Exception({
    val messages =
      patternErrors.iterator.map { case (name, ex) =>
        val msg = ex.getMessage.drop {
          "Text '' could not be parsed,".length + content.length
        }.trim
        s"$name: $msg"
      }
    s"'$content' did not match any of the following formats:\n- ${messages.mkString("\n- ")}"
  })
  with NoStackTrace
