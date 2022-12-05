package org.dsasf.members
package database.models

import database.models.national.NationalMembershipRecord

import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import io.getquill.{MappedEncoding, query, quote}
import shapeless.tag.@@
import zio.ZIO

import java.util.UUID
import scala.collection.mutable

// TODO: Move these to a models project

final case class UserId(toUuid: UUID) extends AnyVal with DbId
object UserId extends DbIdCompanion[UserId]

final case class User(id: UserId, fullName: Name, primaryEmailAddress: EmailAddress)