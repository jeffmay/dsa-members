package org.dsasf.members
package database

import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import eu.timepit.refined.string.ValidBigInt
import io.estatico.newtype.macros.newtype

package object models {

  @newtype case class Name(asString: String)

  type NumericString = String Refined NumericString.Valid
  object NumericString {
    type Valid = ValidBigInt

    def apply(text: String): NumericString = {
      refineV[ValidBigInt].unsafeFrom(text.replaceAll("\\D", ""))
    }
  }

}
