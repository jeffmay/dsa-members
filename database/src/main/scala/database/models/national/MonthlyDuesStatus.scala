package org.dsasf.members
package database.models.national

import enumeratum.ops.{EnumCodec, EnumCompanion}
import enumeratum.values.{StringEnum, StringEnumEntry}

sealed abstract class MonthlyDuesStatus(override val value: String)
  extends StringEnumEntry

object MonthlyDuesStatus
  extends EnumCompanion[MonthlyDuesStatus] with StringEnum[MonthlyDuesStatus] {
  implicit override val codec: EnumCodec[MonthlyDuesStatus] =
    EnumCodec.fromEnum(this)
  override val values: IndexedSeq[MonthlyDuesStatus] = findValues

  final case object Active extends MonthlyDuesStatus("active")
  final case object Never extends MonthlyDuesStatus("never")
}
