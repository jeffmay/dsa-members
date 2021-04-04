package org.dsasf.members
package database.models.national

import database.models.{EnumCodec, IsEnum}

import enumeratum.values.{StringEnum, StringEnumEntry}

sealed abstract class MonthlyDuesStatus(override val value: String)
  extends StringEnumEntry

object MonthlyDuesStatus
  extends IsEnum[MonthlyDuesStatus] with StringEnum[MonthlyDuesStatus] {
  implicit override val codec: EnumCodec[MonthlyDuesStatus] =
    EnumCodec.fromEnum(this)
  override val values: IndexedSeq[MonthlyDuesStatus] = findValues

  final case object Active extends MonthlyDuesStatus("active")
  final case object Never extends MonthlyDuesStatus("never")
}
