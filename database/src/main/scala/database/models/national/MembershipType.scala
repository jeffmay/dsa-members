package org.dsasf.members
package database.models.national

import enumeratum.ops.ValueEnumCompanion
import enumeratum.values.{StringEnum, StringEnumEntry}

sealed abstract class MembershipType(override val value: String)
  extends StringEnumEntry

object MembershipType
  extends ValueEnumCompanion[MembershipType] with StringEnum[MembershipType] {
  override val values: IndexedSeq[MembershipType] = findValues

  final case object Monthly extends MembershipType("monthly")
  final case object Annual extends MembershipType("annual")
  final case object Lapsed extends MembershipType("lapsed")
  final case object CanceledByAdmin extends MembershipType("canceled_by_admin")
  final case object CanceledByFailure
    extends MembershipType("canceled_by_failure")
  final case object TwoMonthsOldPlusFailed
    extends MembershipType("2mo_plus_failed")
  final case object CanceledByProcessor
    extends MembershipType("canceled_by_processor")
}
