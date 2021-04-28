package org.dsasf.members
package database.models.national

import database.models.{Address, Name, PhoneNumber}

import io.estatico.newtype.macros.newtype

import java.time.LocalDate

case class NationalMembershipRecord(
  id: NationalMembershipRecord.Id,
  akId: AkID,
  name: Name,
  billingAddress: Address,
  mailingAddress: Address,
  phoneNumbers: Set[PhoneNumber],
  mailPreference: MailPreference,
  doNotCall: Boolean,
  joinDate: LocalDate,
  expiryDate: LocalDate,
  membershipType: MembershipType,
  monthlyDuesStatus: MonthlyDuesStatus,
  membershipStatus: MembershipStatus,
)

object NationalMembershipRecord {

  @newtype case class Id(toInt: Int)

  implicit val naturalOrder: Ordering[NationalMembershipRecord] =
    Ordering.by(_.id.toInt)
}
