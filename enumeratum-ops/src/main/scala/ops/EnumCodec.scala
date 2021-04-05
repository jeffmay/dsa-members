package enumeratum
package ops

import values.{ValueEnum, ValueEnumEntry}

trait EnumCodec[E] {

  /** The list of all possible enum values
    */
  def values: IndexedSeq[E]

  /** Returns the unique name for an enum value.
    *
    * @note Calling [[findByNameInsensitiveOpt]] with the returned value must always be defined.
    */
  def nameOf(entry: E): String

  /** Finds the enum value for the given name with case insensitivity.
    */
  def findByNameInsensitiveOpt(name: String): Option[E]

  /** Finds the enum value for the given name with case insensitivity or returns an error.
    */
  def findByNameInsensitiveEither(name: String): Either[NoSuchMember, E] = {
    findByNameInsensitiveOpt(name).toRight {
      NoSuchMember(valueNames, name)
    }
  }

  /** The names for all the values in the enumeration.
    *
    * This is handy for debugging what values are possible in an enumeration.
    *
    * @note the value names are not case sensitive and the way that [[findByNameInsensitiveOpt]]
    *       looks up the value must include the strings in this list, but is not limited to it.
    *       In other words, the codec must be lenient enough to find these strings, but it can
    *       be even more lenient and map other names to values.
    */
  lazy val valueNames: IndexedSeq[String] = values.map(nameOf)
}

object EnumCodec {

  @inline def apply[E : EnumCodec]: EnumCodec[E] = implicitly

  def fromEnum[E <: EnumEntry](
    enum: Enum[E],
  ): EnumCodec[E] =
    new EnumCodec[E] {
      final override def values: IndexedSeq[E] = enum.values
      final override def nameOf(entry: E): String = entry.entryName
      final override def findByNameInsensitiveOpt(name: String): Option[E] =
        enum.withNameInsensitiveOption(name)
    }

  def fromEnum[E <: ValueEnumEntry[V], V](
    enum: ValueEnum[V, E],
  ): EnumCodec[E] =
    new EnumCodec[E] {
      final private[this] lazy val lookupLowerCase =
        enum.valuesToEntriesMap.map { case (k, v) ⇒
          (k.toString.toLowerCase, v)
        }
      final override def values: IndexedSeq[E] = enum.values
      final override def nameOf(entry: E): String = entry.value.toString
      final override def findByNameInsensitiveOpt(str: String): Option[E] =
        lookupLowerCase.get(str.toLowerCase)
    }
}
