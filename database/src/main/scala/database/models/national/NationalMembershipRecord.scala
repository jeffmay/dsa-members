package org.dsasf.members
package database.models.national

import database.models.{Address, EmailAddress, Name, PhoneNumber, User, UserId}

import io.getquill.MappedEncoding

import java.time.LocalDate

opaque type NationalMembershipRecordId = Int

implicit object NationalMembershipRecordId extends Conversion[NationalMembershipRecordId, NationalMembershipRecordIdOps] {
  inline def fromInt(inline value: Int): NationalMembershipRecordId = value

  override def apply(id: NationalMembershipRecordId): NationalMembershipRecordIdOps = NationalMembershipRecordIdOps(id)
}

final class NationalMembershipRecordIdOps(val value: Int) extends AnyVal


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

  given MappedEncoding[NationalMembershipRecordId, Int] = MappedEncoding(_.value)
  given MappedEncoding[Int, NationalMembershipRecordId] = MappedEncoding(NationalMembershipRecordId.fromInt(_))
}
