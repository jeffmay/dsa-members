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
}
