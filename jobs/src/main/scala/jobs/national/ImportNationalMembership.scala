package org.dsasf.members
package jobs.national

import com.github.tototoshi.csv.CSVFormat
import zio.URIO
import zio.blocking.Blocking
import zio.csv.{CsvDecoder, CsvParser, ReadingFailure}
import zio.stream.{ZSink, ZStream}

import java.nio.file.Path

object ImportNationalMembership {

  final case class ImportResults(
    successes: Long = 0,
    failures: Long = 0,
  ) {

    def recordSuccess(record: CsvRecord): ImportResults =
      copy(successes = this.successes + 1)

    def recordFailure(failure: ReadingFailure): ImportResults =
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
      CsvDecoder.decodeRowsAs[CsvRecord].usingHeaderInfo(reader)
    decodeRecords.map(Right(_)).catchAll(f => ZStream.succeed(Left(f))).run {
      ZSink.foldLeft(ImportResults()) {
        case (res, Left(failure)) =>
          res.recordFailure(failure)
        case (res, Right(record)) =>
          res.recordSuccess(record)
      }
    }
  }
}
