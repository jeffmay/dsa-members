package org.dsasf.members
package jobs

import database.models.EmailAddress
import database.models.national.MembershipStatus
import jobs.Csv.{CellDecoder, CellDecodingFailure}

import zio.ZIO

trait CommonCsvDecoders {

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

  implicit val decodeMembershipStatus: CellDecoder[MembershipStatus] =
    CellDecoder.fromEffect { str ⇒
      ZIO.fromEither(MembershipStatus.withValueEither(str)).flatMapError { ex ⇒
        CellDecodingFailure.fromExceptionDecodingAs[MembershipStatus](ex)
      }
    }
}
