package org.dsasf.members
package database.models.national

import database.models.{Address, Name, PhoneNumber}

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

  opaque type Id = Int
  object Id {
    def apply(value: Int): Id = value
  }

  extension (id: Id) inline def value: Int = id

  given naturalOrder: Ordering[NationalMembershipRecord] = Ordering.by(_.id.value)
}
