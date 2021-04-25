package org.dsasf.members
package database.models.national

import database.models.{Address, Name, PhoneNumber}

import io.estatico.newtype.macros.newtype

case class NationalMembershipRecord(
  id: NationalMembershipRecord.Id,
  akId: AkID,
  name: Name,
  billingAddress: Address,
  mailingAddress: Address,
  phoneNumbers: Set[PhoneNumber],
  //  emailAddress: Option[EmailAddress],
  //  mailPreference: UnknownEntryOr[MailPreference],
  //  doNotCall: Boolean,
  //  joinDate: LocalDate,
  //  expiryDate: LocalDate,
  //  membershipType: UnknownEntryOr[MembershipType],
  //  monthlyDuesStatus: UnknownEntryOr[MonthlyDuesStatus],
  //  membershipStatus: UnknownEntryOr[MembershipStatus],
)

object NationalMembershipRecord {

  @newtype case class Id(toInt: Int)

  implicit val naturalOrder: Ordering[NationalMembershipRecord] =
    Ordering.by(_.id.toInt)
}
