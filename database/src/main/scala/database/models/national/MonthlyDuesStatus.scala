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
  final case object CanceledByAdmin
    extends MonthlyDuesStatus("canceled_by_admin")
  final case object Never extends MonthlyDuesStatus("never")
}
