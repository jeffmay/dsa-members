package org.dsasf.members
package jobs.national

import com.github.tototoshi.csv.CSVFormat
import zio.URIO
import zio.blocking.Blocking
import zio.csv.{
  CsvDecoder,
  CsvParser,
  DecodingFailure,
  ParsingFailure,
  ReadingFailure,
}
import zio.stream.{ZSink, ZStream}

import java.nio.file.Path

object ImportNationalMembership {

  final case class ImportResults(
    successCount: Long = 0,
    allFailures: Vector[ReadingFailure] = Vector.empty,
  ) {

    def failureCount: Long = allFailures.size

    lazy val decodingFailures: Vector[DecodingFailure] = allFailures.collect {
      case f: DecodingFailure => f
    }

    lazy val parsingFailures: Vector[ParsingFailure] = allFailures.collect {
      case f: ParsingFailure => f
    }

    def recordSuccess(record: CsvRecord): ImportResults =
      copy(successCount = this.successCount + 1)

    def recordFailure(failure: ReadingFailure): ImportResults =
      copy(allFailures = allFailures :+ failure)

  }

  final case class ImportFailed(
    results: ImportResults,
  )

  def fromCsvFile(
    path: Path,
    format: CSVFormat,
  ): URIO[Blocking, ImportResults] = {
    val reader = CsvParser.fromFile(path, format)
    val decodeRecords = CsvDecoder
      .decodeRowsAsEitherFailureOr[CsvRecord].usingHeaderInfo(reader)
      .catchAll(f => ZStream.succeed(Left(f)))
    decodeRecords.run {
      ZSink.foldLeft(ImportResults()) {
        case (res, Left(failure)) =>
          res.recordFailure(failure)
        case (res, Right(record)) =>
          res.recordSuccess(record)
      }
    }
  }
}
