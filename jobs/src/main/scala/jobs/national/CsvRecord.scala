package org.dsasf.members
package jobs.national

import database.models.national._
import database.models.{Address, EmailAddress, NameComponentsUsa, PhoneNumber}
import jobs.{national, CommonDecoders}

import zio.csv.{CellDecoder, RowDecoder}

import java.time.LocalDate

final case class CsvRecord(
  akId: AkID,
  name: NameComponentsUsa,
  billingAddress: Address,
  mailingAddress: Address,
  mobilePhone: Seq[PhoneNumber],
  homePhone: Seq[PhoneNumber],
  workPhone: Seq[PhoneNumber],
  emailAddress: Option[EmailAddress],
  mailPreference: MailPreference,
  doNotCall: Boolean,
  joinDate: LocalDate,
  expiryDate: LocalDate,
  membershipType: Option[MembershipType],
  monthlyDuesStatus: MonthlyDuesStatus,
  membershipStatus: MembershipStatus,
)

object CsvRecord extends CommonDecoders {

  final object Keys {
    final val AK_ID = "AK_ID"
    final val FIRST_NAME = "first_name"
    final val MIDDLE_NAME = "middle_name"
    final val LAST_NAME = "last_name"
    final val SUFFIX = "suffix"
    final val BILLING_ADDRESS_LINE_1 = "Billing_Address_Line_1"
    final val BILLING_ADDRESS_LINE_2 = "Billing_Address_Line_2"
    final val BILLING_CITY = "Billing_City"
    final val BILLING_STATE = "Billing_State"
    final val BILLING_ZIP = "Billing_Zip"
    final val MAILING_ADDRESS_LINE_1 = "Mailing_Address1"
    final val MAILING_ADDRESS_LINE_2 = "Mailing_Address2"
    final val MAILING_CITY = "Mailing_City"
    final val MAILING_STATE = "Mailing_State"
    final val MAILING_ZIP = "Mailing_Zip"
    final val MOBILE_PHONE = "Mobile_Phone"
    final val HOME_PHONE = "Home_Phone"
    final val WORK_PHONE = "Work_Phone"
    final val EMAIL = "Email"
    final val MAIL_PREFERENCE = "Mail_preference"
    final val DO_NOT_CALL = "Do_Not_Call"
    final val JOIN_DATE = "Join_Date"
    final val EXPIRY_DATE = "Xdate"
    final val MEMBERSHIP_TYPE = "membership_type"
    final val MONTHLY_DUES_STATUS = "monthly_dues_status"
    final val MEMBERSHIP_STATUS = "membership_status"
    final val UNION_MEMBER = "union_member"
    final val UNION_NAME = "union_name"
    final val UNION_LOCAL = "union_local"
    final val STUDENT_STATUS = "student_yes_no"
    final val STUDENT_SCHOOL_NAME = "student_school_name"
    final val YDSA_CHAPTER = "YDSA Chapter"
    final val DSA_CHAPTER = "DSA_chapter"
  }

  implicit val decodeAkID: CellDecoder[AkID] =
    CellDecoder.fromStringTotal(AkID(_))

  implicit val decodeWithHeaders: RowDecoder.FromHeaderInfo[CsvRecord] = {
    row =>
      for {
        akId <- row(Keys.AK_ID).as[AkID]
        firstName <- row(Keys.FIRST_NAME).asString
        middleName <- row(Keys.MIDDLE_NAME).asString
        lastName <- row(Keys.LAST_NAME).asString
        suffix <- row(Keys.SUFFIX).asString
        billingAddressLine1 <- row(Keys.BILLING_ADDRESS_LINE_1).asString
        billingAddressLine2 <- row(Keys.BILLING_ADDRESS_LINE_2).asString
        billingCity <- row(Keys.BILLING_CITY).asString
        billingState <- row(Keys.BILLING_STATE).asString
        billingZip <- row(Keys.BILLING_ZIP).asString
        mailingAddressLine1 <- row(Keys.MAILING_ADDRESS_LINE_1).asString
        mailingAddressLine2 <- row(Keys.MAILING_ADDRESS_LINE_2).asString
        mailingCity <- row(Keys.MAILING_CITY).asString
        mailingState <- row(Keys.MAILING_STATE).asString
        mailingZip <- row(Keys.MAILING_ZIP).asString
        mobilePhone <- row(Keys.MOBILE_PHONE).as[Seq[PhoneNumber]]
        homePhone <- row(Keys.HOME_PHONE).as[Seq[PhoneNumber]]
        workPhone <- row(Keys.WORK_PHONE).as[Seq[PhoneNumber]]
        emailAddress <- row(Keys.EMAIL).as[Option[EmailAddress]]
        mailPreference <-
          row(Keys.MAIL_PREFERENCE).as[MailPreference]
        doNotCall <- row(Keys.DO_NOT_CALL).as[Boolean]
        joinDate <- row(Keys.JOIN_DATE).as[LocalDate]
        expiryDate <- row(Keys.EXPIRY_DATE).as[LocalDate]
        membershipType <-
          row(Keys.MEMBERSHIP_TYPE).as[Option[MembershipType]]
        monthlyDuesStatus <-
          row(Keys.MONTHLY_DUES_STATUS).as[MonthlyDuesStatus]
        membershipStatus <-
          row(Keys.MEMBERSHIP_STATUS).as[MembershipStatus]
      } yield national.CsvRecord(
        akId,
        NameComponentsUsa(
          firstName,
          middleName,
          lastName,
          suffix,
        ),
        Address(
          billingAddressLine1,
          billingAddressLine2,
          billingCity,
          billingState,
          billingZip,
        ),
        Address(
          mailingAddressLine1,
          mailingAddressLine2,
          mailingCity,
          mailingState,
          mailingZip,
        ),
        mobilePhone,
        homePhone,
        workPhone,
        emailAddress,
        mailPreference,
        doNotCall,
        joinDate,
        expiryDate,
        membershipType,
        monthlyDuesStatus,
        membershipStatus,
      )
  }
}
