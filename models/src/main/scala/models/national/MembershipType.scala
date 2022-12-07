package org.dsasf.members
package models.national

enum MembershipType(val value: String):
  case Monthly extends MembershipType("monthly")
  case Annual extends MembershipType("annual")
