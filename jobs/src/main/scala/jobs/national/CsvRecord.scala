package org.dsasf.members
package jobs.national

import database.models.national._
import database.models.{Address, EmailAddress, NameComponentsUsa, PhoneNumber}
import jobs.{national, CommonDecoders}

import zio.Tag
import zio.csv.{CellDecoder, HeaderCtx, RowCtx, RowDecoder}

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
        akId <- row.cellAs[AkID](Keys.AK_ID)
        firstName <- row.cellAs[String](Keys.FIRST_NAME)
        middleName <- row.cellAs[String](Keys.MIDDLE_NAME)
        lastName <- row.cellAs[String](Keys.LAST_NAME)
        suffix <- row.cellAs[String](Keys.SUFFIX)
        billingAddressLine1 <- row.cellAs[String](Keys.BILLING_ADDRESS_LINE_1)
        billingAddressLine2 <- row.cellAs[String](Keys.BILLING_ADDRESS_LINE_2)
        billingCity <- row.cellAs[String](Keys.BILLING_CITY)
        billingState <- row.cellAs[String](Keys.BILLING_STATE)
        billingZip <- row.cellAs[String](Keys.BILLING_ZIP)
        mailingAddressLine1 <- row.cellAs[String](Keys.MAILING_ADDRESS_LINE_1)
        mailingAddressLine2 <- row.cellAs[String](Keys.MAILING_ADDRESS_LINE_2)
        mailingCity <- row.cellAs[String](Keys.MAILING_CITY)
        mailingState <- row.cellAs[String](Keys.MAILING_STATE)
        mailingZip <- row.cellAs[String](Keys.MAILING_ZIP)
        mobilePhone <- row.cellAs[Seq[PhoneNumber]](Keys.MOBILE_PHONE)
        homePhone <- row.cellAs[Seq[PhoneNumber]](Keys.HOME_PHONE)
        workPhone <- row.cellAs[Seq[PhoneNumber]](Keys.WORK_PHONE)
        emailAddress <- row.cellAs[Option[EmailAddress]](Keys.EMAIL)
        mailPreference <-
          row.cell(Keys.MAIL_PREFERENCE).flatMap(
            _.contentAs[MailPreference](CellDecoder.fromEnum[MailPreference]),
          )
        doNotCall <- row.cellAs[Boolean](Keys.DO_NOT_CALL)
        joinDate <- row.cell(Keys.JOIN_DATE).flatMap(_.contentAs[LocalDate])
        expiryDate <- row.cellAs[LocalDate](Keys.EXPIRY_DATE)
        membershipType <-
          row.cell(Keys.MEMBERSHIP_TYPE).flatMap(_.contentAs[Option[
            MembershipType,
          ]](CellDecoder.optional(CellDecoder.fromEnum[MembershipType])))
        monthlyDuesStatus <-
          row.cell(Keys.MONTHLY_DUES_STATUS).flatMap(_.contentAs[
            MonthlyDuesStatus,
          ](CellDecoder.fromEnum[MonthlyDuesStatus]))
        membershipStatus <- row.cell(Keys.MEMBERSHIP_STATUS).flatMap(
          _.contentAs[MembershipStatus](CellDecoder.fromEnum[MembershipStatus]),
        )
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
