package org.dsasf.members
package jobs

import database.models.{EmailAddress, IsEnum}
import jobs.Csv.CellDecoder

trait CommonDecoders {

  implicit def decodeUnknownEntryOr[
    E : IsEnum : CellDecoder,
  ]: CellDecoder[
    UnknownEntryOr[E],
  ] = CellDecoder.fromEffect { str ⇒
    CellDecoder[E].decodeString(str)
      .fold(
        _ ⇒ UnknownEntryOr[E].fromUnknown(str),
        UnknownEntryOr[E].fromKnown,
      )
  }

  implicit val decodeEmailAddress: CellDecoder[EmailAddress] =
    CellDecoder.fromStringSafe { cell ⇒
      require(
        cell.nonEmpty,
        s"EmailAddress cannot be empty.",
      )
      val firstAtSymbol = cell.indexOf('@')
      require(
        firstAtSymbol >= 0,
        s"EmailAddress '$cell' requires an '@' symbol. None found.",
      )
      require(
        firstAtSymbol > 0,
        s"EmailAddress '$cell' requires a username before the '@' symbol.",
      )
      val username = cell.take(firstAtSymbol)
      require(
        username.nonEmpty,
        s"EmailAddress '$cell' username cannot be empty.",
      )
      val domain = cell.drop(firstAtSymbol + 1)
      require(
        domain.nonEmpty,
        s"EmailAddress '$cell' domain cannot be empty.",
      )
      require(
        !domain.contains('@'),
        s"EmailAddress domain '$domain' cannot contain an '@' symbol. First symbol found at index $firstAtSymbol.",
      )
      // TODO: Validate domain with regex? Use refined?
      EmailAddress(username, domain)
    }
}
