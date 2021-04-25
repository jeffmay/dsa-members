package org.dsasf.members
package database.models

import database.models.national.NationalMembershipRecord

import eu.timepit.refined.collection.NonEmpty
import shapeless.tag.@@

import scala.collection.mutable

sealed trait BaseUserFields {
  def fullName: Name
  def nickNames: Set[Name]
  def emailAddress: EmailAddress
  def membershipRecords: Seq[NationalMembershipRecord]
}

sealed trait MemberFields extends BaseUserFields {
  override def membershipRecords: Seq[NationalMembershipRecord] @@ NonEmpty
  def latestMembership: NationalMembershipRecord
}

final case class Member(
  fullName: Name,
  nickNames: Set[Name],
  emailAddress: EmailAddress,
  membershipRecords: Seq[NationalMembershipRecord] @@ NonEmpty,
) extends MemberFields {
  override lazy val latestMembership: NationalMembershipRecord =
    mutable.ArraySeq.from(membershipRecords)
      .sortInPlace()
      .head
}
