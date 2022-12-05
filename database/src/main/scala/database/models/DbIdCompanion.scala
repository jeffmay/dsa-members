package org.dsasf.members
package database.models

import io.getquill.MappedEncoding

import java.util.UUID

trait DbId extends Any {
  def toUuid: UUID
}

trait DbIdCompanion[Id <: DbId] {

  def apply(uuid: UUID): Id

  given MappedEncoding[UUID, Id] = MappedEncoding(apply)

  given MappedEncoding[Id, UUID] = MappedEncoding(_.toUuid)
}
