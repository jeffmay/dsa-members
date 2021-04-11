package org.dsasf.members
package jobs.national

import com.github.tototoshi.csv.CSVFormat
import zio.{URIO, ZIO}
import zio.blocking.Blocking
import zio.csv.{CsvDecoder, CsvParser, ReadingFailure, RowFailure}
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
    val decodeRecords = CsvDecoder.decodeRowsUsingHeaderInfoAs[CsvRecord](reader)
    // TODO: How to count errors?
    decodeRecords.run {
      ZSink.foldLeft(ImportResults()) {
        case (res, record) ⇒
          res.recordSuccess(record)
      }
    }.orDie
  }
}
