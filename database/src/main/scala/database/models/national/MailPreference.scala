package org.dsasf.members
package database.models.national

import database.models.{EnumCodec, IsEnum}

import enumeratum.values.{StringEnum, StringEnumEntry}

sealed abstract class MailPreference(override val value: String)
  extends StringEnumEntry

object MailPreference
  extends IsEnum[MailPreference] with StringEnum[MailPreference] {
  implicit override val codec: EnumCodec[MailPreference] =
    EnumCodec.fromEnum(this)
  override val values: IndexedSeq[MailPreference] = findValues

  final case object MemberCardOnly
    extends MailPreference("Membership card only")
  final case object Yes extends MailPreference("Yes")
  final case object No extends MailPreference("No")
}