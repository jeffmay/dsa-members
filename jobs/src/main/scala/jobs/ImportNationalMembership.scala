package org.dsasf.members
package jobs

import jobs.Csv.{CellDecoder, RowFailure}

object ImportNationalMembership {

  final case class Name(
    firstName: String,
    middleName: String,
    lastName: String,
    suffix: String,
  )

  final case class Address(
    line1: String,
    line2: String,
    city: String,
    state: String,
    zip: String,
  )

  final case class EmailAddress(
    username: String,
    domain: String,
  ) {
    val fullAddress: String = s"$username@$domain"
    override def toString: String = fullAddress
  }

  final object EmailAddress {
    implicit val cellDecoder: CellDecoder[EmailAddress] =
      CellDecoder.fromTry { cell ⇒
        val firstAtSymbol = cell.indexOf('@')
        val (username, domain) = cell.splitAt(firstAtSymbol)
        require(
          !domain.contains('@'),
          "EmailAddress cannot contain multiple '@' symbols",
        )
        // TODO: Validate domain with regex?
        EmailAddress(username, domain)
      }
  }

  final case class NationalCsvRecord(
    akId: String,
    name: Name,
    billingAddress: Address,
    mailingAddress: Address,
    mobilePhone: String,
    homePhone: String,
    workPhone: String,
  )

  final object NationalCsvRecord {

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

    implicit val decodeWithHeaders: Csv.RowDecoder.FromHeaderCtx[
      NationalCsvRecord,
    ] = { row ⇒
      for {
        akId ← row(Keys.AK_ID).asString
        firstName ← row(Keys.FIRST_NAME).asString
        middleName ← row(Keys.MIDDLE_NAME).asString
        lastName ← row(Keys.LAST_NAME).asString
        suffix ← row(Keys.SUFFIX).asString
        billingAddressLine1 ← row(Keys.BILLING_ADDRESS_LINE_1).asString
        billingAddressLine2 ← row(Keys.BILLING_ADDRESS_LINE_2).asString
        billingCity ← row(Keys.BILLING_CITY).asString
        billingState ← row(Keys.BILLING_STATE).asString
        billingZip ← row(Keys.BILLING_ZIP).asString
        mailingAddressLine1 ← row(Keys.MAILING_ADDRESS_LINE_1).asString
        mailingAddressLine2 ← row(Keys.MAILING_ADDRESS_LINE_2).asString
        mailingCity ← row(Keys.MAILING_CITY).asString
        mailingState ← row(Keys.MAILING_STATE).asString
        mailingZip ← row(Keys.MAILING_ZIP).asString
        mobilePhone ← row(Keys.MOBILE_PHONE).asString
        homePhone ← row(Keys.HOME_PHONE).asString
        workPhone ← row(Keys.WORK_PHONE).asString
        email ← row(Keys.EMAIL).as[EmailAddress]
        mailPreference ← row(Keys.MAIL_PREFERENCE).asString
        doNotCall ← row(Keys.DO_NOT_CALL).asString
        joinDate ← row(Keys.JOIN_DATE).asString
      } yield NationalCsvRecord(
        akId,
        Name(
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
      )
    }
  }

  final case class ImportResults(
    successes: Long = 0,
    failures: Long = 0,
  ) {

    def recordSuccess(record: NationalCsvRecord): ImportResults =
      copy(successes = this.successes + 1)

    def recordFailure(failure: RowFailure): ImportResults =
      copy(successes = this.failures + 1)
  }

  final case class ImportFailed(
    results: ImportResults,
  )

//  def run(
//    records: UStream[String],
//  ): ZIO[Console, ImportFailed, ImportResults] = {
//    val allRecordsOrError = Csv.parseWithHeaderAs[Record].fromLines(records)
//    val allRecordsAccumErrors = allRecordsOrError.bimap()
//    // TODO: Configure parallelism here
//    val y = allRecordsOrErrors.scan(ImportResults()) {
//      (res, record) ⇒ res.recordSuccess(record)
//    }
//    val x = allRecordsOrErrors.mapMParUnordered(10) { record ⇒ }
//    x
//  }

}
