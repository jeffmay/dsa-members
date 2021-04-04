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
    CellDecoder.fromTry { cell ⇒
      val firstAtSymbol = cell.indexOf('@')
      val (username, domain) = cell.splitAt(firstAtSymbol)
      require(
        !domain.contains('@'),
        "EmailAddress cannot contain multiple '@' symbols",
      )
      // TODO: Validate domain with regex? Use refined?
      EmailAddress(username, domain)
    }
}
