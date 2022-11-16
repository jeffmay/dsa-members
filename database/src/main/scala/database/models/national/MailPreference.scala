package org.dsasf.members
package database.models.national

import enumeratum.ops.ValueEnumCompanion
import enumeratum.values.{StringEnum, StringEnumEntry}

sealed abstract class MailPreference(override val value: String)
  extends StringEnumEntry

object MailPreference
  extends StringEnum[MailPreference] with ValueEnumCompanion[MailPreference] {
  override val values: IndexedSeq[MailPreference] = findValues

  final case object MemberCardOnly
    extends MailPreference("Membership card only")
  final case object Yes extends MailPreference("Yes")
  final case object No extends MailPreference("No")
}
