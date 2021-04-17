package org.dsasf.members
package jobs.national

import com.github.tototoshi.csv.CSVFormat
import zio.{URIO, ZIO}
import zio.blocking.Blocking
import zio.csv.{CsvDecoder, CsvParser, HeaderCtx, ReadingFailure, RowFailure}
import zio.stream.ZSink

import java.nio.file.Path

object ImportNationalMembership {

  final case class ImportResults(
    successes: Long = 0,
    failures: Long = 0,
  ) {

    def recordSuccess(record: CsvRecord): ImportResults =
      copy(successes = this.successes + 1)

    def recordFailure(failure: RowFailure): ImportResults =
      copy(failures = this.failures + 1)
  }

  final case class ImportFailed(
    results: ImportResults,
  )

  def fromCsvFile(
    path: Path,
    format: CSVFormat,
  ): URIO[Blocking, ImportResults] = {
    val reader = CsvParser.fromFile(path, format)
    val decodeRecords =
      CsvDecoder.decodeRowsAs[CsvRecord].providedHeader(
        headerInfo,
        reader.drop(1),
      )
    // TODO: How to count errors?
    decodeRecords.run {
      ZSink.foldLeft(ImportResults()) {
        case (res, record) ⇒
          res.recordSuccess(record)
      }
    }.orDie
  }

  private val headerInfo: HeaderCtx = HeaderCtx(Map(
    "Mail_preference" → 19,
    "first_name" → 1,
    "Xdate" → 22,
    "Mailing_Address2" → 11,
    "union_local" → 28,
    "DSA_chapter" → 32,
    "Mailing_State" → 13,
    "Join_Date" → 21,
    "Mailing_Address1" → 10,
    "Mobile_Phone" → 15,
    "Billing_State" → 8,
    "Do_Not_Call" → 20,
    "Billing_Zip" → 9,
    "membership_status" → 25,
    "monthly_dues_status" → 24,
    "Mailing_Zip" → 14,
    "membership_type" → 23,
    "suffix" → 4,
    "student_yes_no" → 29,
    "union_member" → 26,
    "student_school_name" → 30,
    "Email" → 18,
    "Billing_Address_Line_2" → 6,
    "YDSA Chapter" → 31,
    "last_name" → 3,
    "Work_Phone" → 17,
    "Billing_Address_Line_1" → 5,
    "middle_name" → 2,
    "Mailing_City" → 12,
    "Home_Phone" → 16,
    "union_name" → 27,
    "Billing_City" → 7,
    "AK_ID" → 0,
  ))
}
