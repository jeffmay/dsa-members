package org.dsasf.members
package models.national

import models.*

import java.time.LocalDate

final case class NationalMembershipRecord(
  id: NationalMembershipRecordId,
  userId: Option[UserId],
  akId: AkID,
  name: Name,
  billingAddress: Address,
  mailingAddress: Address,
  emailAddress: Option[EmailAddress],
  phoneNumbers: Seq[PhoneNumber],
  mailPreference: MailPreference,
  doNotCall: Boolean,
  joinDate: LocalDate,
  expiryDate: LocalDate,
  membershipType: MembershipType,
  monthlyDuesStatus: MonthlyDuesStatus,
  membershipStatus: MembershipStatus,
)

object NationalMembershipRecord {

  given naturalOrder: Ordering[NationalMembershipRecord] = Ordering.by(_.id.value)
}

type NationalMembershipRecordId = NationalMembershipRecordId.NationalMembershipRecordIdValue
object NationalMembershipRecordId {
  opaque type NationalMembershipRecordIdValue = Int

  inline def apply(inline value: Int): NationalMembershipRecordId = value
  extension (inline id: NationalMembershipRecordIdValue) inline def value: Int = id
}
