package org.dsasf.members
package database.models.national

import database.models.{EnumCodec, IsEnum}

import enumeratum.values.{StringEnum, StringEnumEntry}

sealed abstract class MembershipStatus(override val value: String)
  extends StringEnumEntry

object MembershipStatus
  extends IsEnum[MembershipStatus] with StringEnum[MembershipStatus] {
  implicit override val codec: EnumCodec[MembershipStatus] =
    EnumCodec.fromEnum(this)
  override val values: IndexedSeq[MembershipStatus] = findValues

  final case object Member extends MembershipStatus("member")
  final case object MemberInGoodStanding
    extends MembershipStatus("member in good standing")
  final case object Expired extends MembershipStatus("expired")
}
