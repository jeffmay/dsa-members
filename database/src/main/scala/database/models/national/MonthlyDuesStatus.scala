package org.dsasf.members
package database.models.national

import enumeratum.ops.ValueEnumCompanion
import enumeratum.values.{StringEnum, StringEnumEntry}

sealed abstract class MonthlyDuesStatus(override val value: String)
  extends StringEnumEntry

object MonthlyDuesStatus
  extends ValueEnumCompanion[MonthlyDuesStatus]
  with StringEnum[MonthlyDuesStatus] {
  override val values: IndexedSeq[MonthlyDuesStatus] = findValues

  final case object Active extends MonthlyDuesStatus("active")
  final case object Lapsed extends MonthlyDuesStatus("lapsed")
  final case object PastDue extends MonthlyDuesStatus("past_due")
  final case object TwoMonthAfterFailed
    extends MonthlyDuesStatus("2mo_plus_failed")
  final case object CanceledByAdmin
    extends MonthlyDuesStatus("canceled_by_admin")
  final case object CanceledByProcessor
    extends MonthlyDuesStatus("canceled_by_processor")
  final case object CanceledByFailure
    extends MonthlyDuesStatus("canceled_by_failure")
  final case object Never extends MonthlyDuesStatus("never")
}
