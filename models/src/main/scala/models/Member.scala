package org.dsasf.members
package models

import national.NationalMembershipRecord

import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty

import scala.collection.mutable

final case class Member(
  user: User,
  membershipRecords: Seq[NationalMembershipRecord] Refined NonEmpty,
) {

  lazy val latestMembership: NationalMembershipRecord =
    mutable.ArraySeq.from(membershipRecords.value)
      .sortInPlace()
      .head
}
