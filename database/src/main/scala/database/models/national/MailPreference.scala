package org.dsasf.members
package database.models.national

enum MailPreference(val value: String):
  case MemberCardOnly extends MailPreference("Membership card only")
  case Yes extends MailPreference("Yes")
  case No extends MailPreference("No")
