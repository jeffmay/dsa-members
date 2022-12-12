package org.dsasf.members
package models.national

enum MembershipStatus(val value: String):
  case Member extends MembershipStatus("member")
  case MemberInGoodStanding extends MembershipStatus("member in good standing")
  case Expired extends MembershipStatus("expired")