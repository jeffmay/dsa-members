package org.dsasf.members
package database.models

import eu.timepit.refined.api.Refined
import eu.timepit.refined.predicates.all._
import eu.timepit.refined._

sealed trait PhoneNumber extends Any {
  // def format: PhoneNumber.Format
  def toLong: Long
  def show: String
  // override def toString: String = toString(format)
}

object PhoneNumber {

  // opaque type CountryCode = Long
  // object CountryCode {
  //   // TODO: Validate
  //   def apply(code: Int): CountryCode = code

  //   final val USA: CountryCode = 1
  // }

  // enum Format {
  //   case Dots
  //   case Dashes
  //   case ParensAndDashes

  //   def format(number: PhoneNumber): String = this match {
  //     case Dots => number.toLong
  //   }
  // }

  def parse(text: String): Either[String, PhoneNumber] = {
    NumericString.parse(text).flatMap { numeric =>
      PhoneNumberUSA.fromNumericString(numeric).left.map { err =>
        s"Unrecognized PhoneNumber format for '$text':\nX Domestic - $err"
      }
    }
  }
}

// sealed trait InternationalPhoneNumber extends PhoneNumber {
//   def countryCode: PhoneNumber.CountryCode
// }

// sealed trait DomesticPhoneNumber extends PhoneNumber {
//   def toInternational(countryCode: PhoneNumber.CountryCode): InternationalPhoneNumber
// }

object PhoneNumberUSA {
  final val MinLong =   100_000_0000L
  final val MaxLong = 9_999_999_9999L
  type Compact = Interval.Open[MinLong.type, MaxLong.type]

  // language=RegExp
  final val Valid = """1?[1-9]\d{9}"""
  type Valid = NumericString.Valid And MatchesRegex[Valid.type]

  def fromLong(compact: Long Refined Compact): PhoneNumberUSA =
    new PhoneNumberUSA(compact)

  def fromString(valid: String Refined Valid): PhoneNumberUSA = {
    val compact = valid.value.toLong % (MaxLong + 1)
    new PhoneNumberUSA(
      refineV[Compact](compact)
        .fold(
          _ =>
            // This should never happen if the definitions above are aligned.
            // A string that has all numbers, has 10 digits (excluding any +1 prefix for US country code),
            // and does not start with a '0' should always be between MinLong and MaxLong.
            throw new IllegalStateException(
              s"Implementation for PhoneNumberUSA.Compact and PhoneNumberUSA.Valid do not match for $valid",
            ),
          identity,
        ),
    )
  }

  def parse(text: String): Either[String, PhoneNumber] = {
    refineV[NonEmpty](text).flatMap { nonEmptyText =>
      fromNumericString(NumericString(nonEmptyText))
    }
  }

  def fromNumericString(stripped: NumericString): Either[
    String,
    PhoneNumberUSA,
  ] = {
    val result = stripped.value match {
      case s if (10 to 11).contains(s.length) =>
        val localized = if (s.charAt(0) == '1') s.toLong % MinLong else s.toLong
        refineV[PhoneNumberUSA.Compact](localized).left.map { _ =>
          "Expected a string with 10 digits (not starting with '0') " +
            "OR 11 digits (starting with the USA country code, '1', but not followed by a '0')"
        }
      case _ =>
        Left("Expected a string with 10 digits")
    }
    result.map { number =>
      new PhoneNumberUSA(number)
    }
  }
}

final case class PhoneNumberUSA private (number: Long Refined PhoneNumberUSA.Compact) extends AnyVal
  with PhoneNumber {

  override def toLong: Long = number.value

  def areaCode: String = (toLong / 1000_0000L).toString

  def first3: String = (toLong / 10000L % 1000L).toString

  def last4: String = (toLong % 10000L).toString

  override def show: String = s"($areaCode) $first3-$last4"

  override def toString: String = show
}
