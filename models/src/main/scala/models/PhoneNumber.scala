package org.dsasf.members
package models

import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import com.google.i18n.phonenumbers.{NumberParseException, PhoneNumberUtil}
import com.google.i18n.phonenumbers.Phonenumber
import zio.Chunk

import scala.util.Try

type GooglePhoneNumber = Phonenumber.PhoneNumber

sealed trait PhoneNumberError extends Exception

final case class PhoneNumberParseError(numberToParse: String, defaultRegion: PhoneNumberRegion, cause: NumberParseException)
  extends Exception(s"Could not parse '$numberToParse' as a PhoneNumber with the default region of $defaultRegion.", cause)
  with PhoneNumberError

final case class PhoneNumberInvalidError(phoneNumber: PhoneNumber)
  extends Exception(s"The phone number ${PhoneNumberUtil.getInstance.format(phoneNumber.toGooglePhoneNumber, PhoneNumberFormat.INTERNATIONAL)}")
  with PhoneNumberError

final case class PhoneNumber private (toGooglePhoneNumber: GooglePhoneNumber) extends AnyVal {

  def countryCode: Int = toGooglePhoneNumber.getCountryCode

  def countryCodeSource: PhoneNumber.CountryCodeSource = toGooglePhoneNumber.getCountryCodeSource

  def nationalNumber: Long = toGooglePhoneNumber.getNationalNumber

  def originalInput: Option[String] = Option.when(toGooglePhoneNumber.hasRawInput) {
    toGooglePhoneNumber.getRawInput
  }

  def extension: Option[String] = Option.when(toGooglePhoneNumber.hasExtension) {
    toGooglePhoneNumber.getExtension
  }

  def formatted: String = formatted(PhoneNumber.Format.INTERNATIONAL)

  def formatted(format: PhoneNumber.Format): String = format.format(this)

  override def toString: String = formatted

}

object PhoneNumber {

  type CountryCodeSource = Phonenumber.PhoneNumber.CountryCodeSource
  object CountryCodeSource {
    export Phonenumber.PhoneNumber.CountryCodeSource.*
  }

  opaque type Format = PhoneNumberFormat
  object Format {
    final val E164: Format = PhoneNumberFormat.E164
    final val INTERNATIONAL: Format = PhoneNumberFormat.INTERNATIONAL
    final val NATIONAL: Format = PhoneNumberFormat.NATIONAL
    final val RFC3966: Format = PhoneNumberFormat.RFC3966
  }

  extension (fmt: Format) def format(phoneNumber: PhoneNumber): String =
    Util.format(phoneNumber.toGooglePhoneNumber, fmt)

  type Region = PhoneNumberRegion
  final val Region = PhoneNumberRegion

  private lazy val Util: PhoneNumberUtil = PhoneNumberUtil.getInstance

  def parseAndValidate(number: String, defaultRegion: PhoneNumberRegion): Either[PhoneNumberError, PhoneNumber] =
    try {
      val pn = PhoneNumber(Util.parse(number, defaultRegion.twoLetterCode))
      if (Util.isValidNumber(pn.toGooglePhoneNumber)) Right(pn)
      else Left(PhoneNumberInvalidError(pn))
    } catch {
      case npe: NumberParseException => Left(PhoneNumberParseError(number, defaultRegion, npe))
    }
}
