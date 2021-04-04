package org.dsasf.members
package database.models.national

import database.models.{EnumCodec, IsEnum}

import enumeratum.values.{StringEnum, StringEnumEntry}

sealed abstract class MembershipType(override val value: String)
  extends StringEnumEntry

object MembershipType
  extends IsEnum[MembershipType] with StringEnum[MembershipType] {
  implicit override val codec: EnumCodec[MembershipType] =
    EnumCodec.fromEnum(this)
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
