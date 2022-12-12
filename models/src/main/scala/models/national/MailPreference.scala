package org.dsasf.members
package models.national

import zio.util.EnumCompanionOf

enum MailPreference(val value: String):
  case MemberCardOnly extends MailPreference("Membership card only")
  case Yes extends MailPreference("Yes")
  case No extends MailPreference("No")

object MailPreference extends EnumCompanionOf[MailPreference]
