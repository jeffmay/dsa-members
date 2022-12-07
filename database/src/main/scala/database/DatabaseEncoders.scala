package org.dsasf.members
package database

import models.*
import models.national.*
import util.StringDiff

import io.getquill.MappedEncoding

import java.util.UUID

trait DatabaseEncoders {
  given MappedEncoding[Name, String] = MappedEncoding(_.value)

  given MappedEncoding[String, Name] = MappedEncoding(Name(_))

  given MappedEncoding[UserId, UUID] = MappedEncoding(_.value)

  given MappedEncoding[UUID, UserId] = MappedEncoding(UserId(_))

  given MappedEncoding[EmailAddress, String] = MappedEncoding(_.fullAddress)

  given MappedEncoding[String, EmailAddress] = MappedEncoding(
    EmailAddress.parse(_).fold(
      msg => throw new IllegalStateException(msg),
      identity,
    ),
  )

  given MappedEncoding[InputEmailAddress, String] = MappedEncoding(_.fullAddress)

  given MappedEncoding[String, InputEmailAddress] = MappedEncoding(
    InputEmailAddress.parse(_).fold(
      msg => throw new IllegalStateException(msg),
      identity,
    ),
  )

  given MappedEncoding[EssentialEmailAddress, String] =
    MappedEncoding(_.fullAddress)

  given MappedEncoding[String, EssentialEmailAddress] =
    MappedEncoding(original =>
      InputEmailAddress.parse(original).flatMap { email =>
        val essentialEmail = email.toEssentialEmailAddress
        val diff = StringDiff(essentialEmail.fullAddress, original)
        diff.firstDiff.fold(Right(essentialEmail)) {
          case (idx, leftDiff, rightDiff) =>
            Left(
              s"EssentialEmailAddress in database did not match computed address starting at index=$idx:\n" +
                s"  Expected: '$leftDiff'\n" +
                s"  Observed: '$rightDiff'",
            )
        }
      }.fold(msg => throw new IllegalStateException(msg), identity),
    )

  given MappedEncoding[PhoneNumber, String] = MappedEncoding(_.formatted)

  given (using
    defaultRegion: PhoneNumberRegion,
  ): MappedEncoding[String, PhoneNumber] =
    MappedEncoding(PhoneNumber.parseAndValidate(_, defaultRegion).toTry.get)

  given MappedEncoding[NationalMembershipRecordId, Int] = MappedEncoding(_.value)

  given MappedEncoding[Int, NationalMembershipRecordId] =
    MappedEncoding(NationalMembershipRecordId(_))

}
