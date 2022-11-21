package org.dsasf.members
package database.models

import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import eu.timepit.refined.predicates.all._
import eu.timepit.refined.types.all.NonEmptyString

type NumericString = String Refined NumericString.Valid
object NumericString:
  type Valid = ValidBigInt

  def apply(nonEmptyString: NonEmptyString): NumericString =
    refineV[ValidBigInt].unsafeFrom(nonEmptyString.value.replaceAll("\\D", ""))

  def parse(text: String): Either[String, NumericString] =
    refineV[NonEmpty](text).left.map(_ =>
      "NumericString cannot be empty",
    ).map(apply)
