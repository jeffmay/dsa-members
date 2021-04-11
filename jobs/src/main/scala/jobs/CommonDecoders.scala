package org.dsasf.members
package jobs

import database.models.EmailAddress

import enumeratum.ops.EnumCodec
import zio.csv.CellDecoder

trait CommonDecoders {

  implicit def decodeUnknownEntryOr[
    E : EnumCodec,
  ]: CellDecoder[
    UnknownEntryOr[E],
  ] = CellDecoder.fromStringTotal { str ⇒
    EnumCodec[E].findByNameInsensitiveOpt(str).map { entry ⇒
      UnknownEntryOr[E].fromKnown(entry)
    }.getOrElse {
      UnknownEntryOr[E].fromUnknown(str)
    }
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
