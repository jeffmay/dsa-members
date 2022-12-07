package org.dsasf.members
package models

import com.google.i18n.phonenumbers.PhoneNumberUtil

final case class PhoneNumberRegionCodeUnsupportedError(regionCode: String)
  extends Exception(s"The provided region code '$regionCode' is not one of the supported two-letter region codes.")

final case class PhoneNumberRegion private(twoLetterCode: String) {
  lazy val countryCode: Int = PhoneNumberRegion.Util.getCountryCodeForRegion(twoLetterCode)

  override def toString: String = s"PhoneNumberRegion(\"$twoLetterCode\", \"+$countryCode\")"
}

object PhoneNumberRegion {

  private lazy val Util = PhoneNumberUtil.getInstance

  def parseAndValidate(twoLetterCode: String): Either[PhoneNumberRegionCodeUnsupportedError, PhoneNumberRegion] =
    Option.when(Util.getSupportedRegions.contains(twoLetterCode.toUpperCase)) {
      PhoneNumberRegion(twoLetterCode)
    }.toRight {
      PhoneNumberRegionCodeUnsupportedError(twoLetterCode)
    }
}
