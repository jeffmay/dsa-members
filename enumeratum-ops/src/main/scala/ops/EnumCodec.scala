package enumeratum
package ops

import values.{NoSuchMember => _, _}

import scala.annotation.implicitNotFound
import scala.collection.immutable.ArraySeq

/** Defines generic operations for extracting and serializing enum values to and from strings.
  *
  * Also defines the complete list of possible values for error messages.
  */
@implicitNotFound("Cannot find implicit EnumCodec[${E}]. Does the companion object extend EnumCompanion[${E}]?")
trait EnumCodec[E] {

  /** Name of the enumeration.
    */
  def enumName: String

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
  protected def findByNameInsensitiveOpt(name: String): Option[E]

  /** Finds the enum value for the given name with case insensitivity or returns an error.
    */
  def findByNameInsensitive(name: String): UnknownEntryOr[E] = {
    UnknownEntryOr(findByNameInsensitiveOpt(name).toRight {
      NoSuchMember(enumName, valueNames, name)
    })
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

  private def enumeratumName(cls: Class[_]): String = {
    val n = cls.getName
    val parts = ArraySeq.unsafeWrapArray(n.split(Array('$', '.')))
    parts.dropWhile(_.charAt(0).isLower).mkString(".")
  }

  def nameOfEnum(enum: Enum[_]): String = enumeratumName(enum.getClass)

  def nameOfEnum(enum: ValueEnum[_, _]): String = enumeratumName(enum.getClass)

  def fromEnum[E <: EnumEntry](e: Enum[E]): EnumCodec[E] =
    new EnumCodec[E] {
      final override val enumName: String = nameOfEnum(e)
      final override def values: IndexedSeq[E] = e.values
      final override def nameOf(entry: E): String = entry.entryName
      final override protected def findByNameInsensitiveOpt(
        name: String,
      ): Option[E] =
        e.withNameInsensitiveOption(name)
    }

  def fromValueEnum[E <: ValueEnumEntry[V], V](
    e: ValueEnum[V, E],
  ): EnumCodec[E] =
    new EnumCodec[E] {
      final override val enumName: String = nameOfEnum(e)
      final private[this] lazy val lookupLowerCase =
        e.valuesToEntriesMap.map { case (k, v) =>
          (k.toString.toLowerCase, v)
        }
      final override def values: IndexedSeq[E] = e.values
      final override def nameOf(entry: E): String = entry.value.toString
      final override protected def findByNameInsensitiveOpt(
        str: String,
      ): Option[E] =
        lookupLowerCase.get(str.toLowerCase)
    }
}
