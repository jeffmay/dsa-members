package org.dsasf.members
package models.national

import models.*

import zio.*

import java.time.LocalDate
import java.util.UUID

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
  opaque type NationalMembershipRecordIdValue = UUID

  inline def apply(inline value: UUID): NationalMembershipRecordId = value

  inline def random: UIO[NationalMembershipRecordId] = Random.nextUUID

  extension (inline id: NationalMembershipRecordId) inline def value: UUID = id
}
