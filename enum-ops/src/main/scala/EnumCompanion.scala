package zio.util

import scala.compiletime.*
import scala.deriving.Mirror

import scala.collection.immutable.ArraySeq

trait EnumCompanion:
  type EntryType

  protected def values: Array[EntryType]

  def entries: IndexedSeq[EntryType] = ArraySeq.unsafeWrapArray(values)

  given EnumEntries[EntryType] = EnumEntries.of(this)

trait EnumCompanionOf[E] extends EnumCompanion:
  final override type EntryType = E
