package org.dsasf.members
package database.models

import eu.timepit.refined.boolean._
import eu.timepit.refined.numeric._
import eu.timepit.refined._
import eu.timepit.refined.string._
import shapeless.tag.@@

sealed trait PhoneNumber extends Any {
  def toLong: Long
  def show: String
}

object PhoneNumber {

  def parse(text: String): Either[String, PhoneNumber] = {
    val stripped = NumericString(text)
    DomesticPhoneNumber.fromNumericString(stripped).left.map { err =>
      s"Unrecognized PhoneNumber format for '$text':\nX Domestic - $err"
    }
  }
}

object DomesticPhoneNumber {
  final val MinLong = 100_000_0000L
  final val MaxLong = 9_999_999_9999L
  type Compact = Interval.Open[MinLong.type, MaxLong.type]

  // language=RegExp
  final val Valid = """1?[1-9]\d{9}"""
  type Valid = NumericString.Valid And MatchesRegex[Valid.type]

  def apply(compact: Long @@ Compact): DomesticPhoneNumber =
    new DomesticPhoneNumber(compact)

  def apply(valid: String @@ Valid): DomesticPhoneNumber = {
    val compact = valid.toLong % (MaxLong + 1)
    new DomesticPhoneNumber(
      refineT[Compact](compact)
        .fold(
          _ =>
            // This should never happen if the definitions above are aligned.
            // A string that has all numbers, has 10 digits (excluding any +1 prefix for US country code),
            // and does not start with a '0' should always be between MinLong and MaxLong.
            throw new IllegalStateException(
              s"Implementation for DomesticPhoneNumber.Compact and DomesticPhoneNumber.Valid do not match for $valid",
            ),
          identity,
        ),
    )
  }

  def parse(text: String): Either[String, PhoneNumber] = {
    fromNumericString(NumericString(text))
  }

  def fromNumericString(stripped: NumericString): Either[
    String,
    DomesticPhoneNumber,
  ] = {
    val result = stripped.value match {
      case s if s.length == 10 =>
        refineT[DomesticPhoneNumber.Compact](s.toLong)
      case s if s.length == 11 && s.charAt(0) == '1' =>
        refineT[DomesticPhoneNumber.Compact](s.toLong % MinLong)
      case _ =>
        Left("Expected a string with 10 digits")
    }
    result.map { number =>
      DomesticPhoneNumber(number)
    }
  }
}

class DomesticPhoneNumber(
  override val toLong: Long @@ DomesticPhoneNumber.Compact,
) extends AnyVal
  with PhoneNumber {

  def areaCode: String = (toLong / 1_000_0000L).toString

  def first3: String = (toLong / 1_0000L % 1_000L).toString

  def last4: String = (toLong % 1_0000L).toString

  override def show: String = s"($areaCode) $first3-$last4"

  override def toString: String = show
}
