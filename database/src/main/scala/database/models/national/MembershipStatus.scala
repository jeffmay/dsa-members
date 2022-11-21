package org.dsasf.members
package database.models.national

enum MembershipStatus(value: String):
  case Member extends MembershipStatus("member")
  case MemberInGoodStanding extends MembershipStatus("member in good standing")
  case Expired extends MembershipStatus("expired")
