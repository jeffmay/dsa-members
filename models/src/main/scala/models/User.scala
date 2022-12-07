package org.dsasf.members
package models

import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import shapeless.tag.@@

import java.util.UUID
import scala.collection.mutable

final case class User(
  id: UserId,
  fullName: Name,
  primaryEmailAddress: EmailAddress,
)

type UserId = UserId.UserIdValue
object UserId:
  opaque type UserIdValue = UUID
  inline def apply(inline id: UUID): UserIdValue = id
  extension (inline id: UserIdValue) inline def value: UUID = id
