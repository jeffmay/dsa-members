package org.dsasf.members
package jobs.national

import database.queries.*
import models.national.*
import models.*

import zio.*
import zio.csv.*
import zio.stream.*

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

    def succeeded(records: IndexedSeq[CsvRecord]): ImportResults =
      copy(successCount = this.successCount + records.size)

    def failed(failures: IndexedSeq[ReadingFailure]): ImportResults =
      copy(allFailures = allFailures ++ failures)

  }

  final case class ImportFailed(
    results: ImportResults,
  )

}

class ImportNationalMembership(
  userQueries: UserQueries,
  nationalMembershipQueries: NationalMembershipRecordQueries,
) {
  import ImportNationalMembership.*

  def fromCsvFile(
    path: Path,
    format: CSVFormat,
  ): UIO[ImportResults] = {
    val reader = CsvParser.fromFileWithHeader(path, format)
    // TODO: It should count failures instead of skipping bad rows.
    //       It should also not just fold a file / IO error into a record failure.
    //       Should it propagate the error through?
    val decodeRecords = CsvDecoder
      .decodeRowsAsEitherFailureOr[CsvRecord](reader.collectRight)
//      .absolve
//      .catchAll(f => ZStream.succeed(Left(f))).provideLayer(Scope.default)
    val importResults = decodeRecords.grouped(100).runFoldZIO(ImportResults()) {
      case (importResults, decodeResults) =>
        for {
          _ <- ZIO.foreachDiscard(decodeResults) {
            case Right(record) =>
              ZIO.logInfo(s"Adding record to batch insert transaction: ${record}")
            case Left(failure) => ZIO.logWarning(
                s"Skipping failure row ${failure.rowIndex + 1}: ${failure.reason}",
              )
          }
          (errors, records) = decodeResults.partitionMap(identity)
          userId <- UserId.random
          membershipId <- NationalMembershipRecordId.random
          (users, memberships) = records.map { row =>
            val fullName = Name {
              s"${row.name.firstName} ${row.name.middleName} ${row.name.lastName}, ${row.name.suffix}"
            }
            val user =
              User(
                userId,
                fullName,
                row.emailAddress.get,
              ) // TODO: Skip records without email addresses
            val record = NationalMembershipRecord(
              membershipId,
              None,
              row.akId,
              fullName,
              row.billingAddress,
              row.mailingAddress,
              row.emailAddress,
              row.homePhone ++ row.mobilePhone ++ row.workPhone,
              row.mailPreference,
              row.doNotCall,
              row.joinDate,
              row.expiryDate,
              row.membershipType.getOrElse(MembershipType.Annual),
              row.monthlyDuesStatus,
              row.membershipStatus,
            )
            (user, record)
          }.unzip
          _ <- ZIO.fromFuture(_ => userQueries.batchInsert(users)) <&>
            ZIO.fromFuture(_ =>
              nationalMembershipQueries.batchInsert(memberships),
            )
        } yield importResults.succeeded(records).failed(errors)
    }
    importResults.catchAll(f => ImportResults().failed(Chunk(f)))
  }
}
