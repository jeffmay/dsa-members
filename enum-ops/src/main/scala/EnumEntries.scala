package zio.util

trait EnumEntries[+E]:
  def entries: IndexedSeq[E]

object EnumEntries:
  inline def of(companion: EnumCompanion): EnumEntries[companion.EntryType] =
    new EnumEntries[companion.EntryType]:
      override def entries: IndexedSeq[companion.EntryType] = companion.entries
