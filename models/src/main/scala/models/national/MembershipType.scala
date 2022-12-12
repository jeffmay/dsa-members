package org.dsasf.members
package models.national

import zio.util.EnumCompanionOf

enum MembershipType(val value: String):
  case Monthly extends MembershipType("monthly")
  case Annual extends MembershipType("annual")

object MembershipType extends EnumCompanionOf[MembershipType]
