package org.dsasf.members
package jobs.national

import jobs.Csv.RowFailure

object ImportNationalMembership {

  final case class ImportResults(
    successes: Long = 0,
    failures: Long = 0,
  ) {

    def recordSuccess(record: CsvRecord): ImportResults =
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
