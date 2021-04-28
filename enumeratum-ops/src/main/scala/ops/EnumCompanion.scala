package enumeratum
package ops

import values._

abstract class AnyEnumCompanion[E] {

  /** Cache the codec using [[EnumCodec.fromEnum]](this) */
  protected val cachedCodec: EnumCodec[E]

  /** Exposes the cached codec for more implicit resolution. */
  implicit def codec: EnumCodec[E] = cachedCodec

  /** All possible values of this enumeration.
    *
    * Typically implemented with enumeratum's `findValues` method
    */
  def values: IndexedSeq[E]
}

abstract class EnumCompanion[E <: EnumEntry]
  extends AnyEnumCompanion[E] with Enum[E] {

  /** Cache the codec using [[EnumCodec.fromEnum]](this) */
  override protected val cachedCodec: EnumCodec[E] = EnumCodec.fromEnum(this)
}

import scala.language.existentials
abstract class ValueEnumCompanion[E <: ValueEnumEntry[_]] {
  self: (ValueEnum[x, E] forSome { type x }) =>

  /** Cache the codec using [[EnumCodec.fromEnum]](this) */
  protected val cachedCodec: EnumCodec[E] = {
    val c: EnumCodec[_] = self match {
      case e: IntEnum[e] => EnumCodec.fromValueEnum[e, Int](e)
      case e: LongEnum[e] => EnumCodec.fromValueEnum[e, Long](e)
      case e: ShortEnum[e] => EnumCodec.fromValueEnum[e, Short](e)
      case e: StringEnum[e] => EnumCodec.fromValueEnum[e, String](e)
      case e: ByteEnum[e] => EnumCodec.fromValueEnum[e, Byte](e)
      case e: CharEnum[e] => EnumCodec.fromValueEnum[e, Char](e)
    }
    // We know this is safe because of the existential type restriction on the self-type
    c.asInstanceOf[EnumCodec[E]]
  }

  /** Exposes the cached codec for more implicit resolution. */
  implicit def codec: EnumCodec[E] = cachedCodec

  /** All possible values of this enumeration.
    *
    * Typically implemented with enumeratum's `findValues` method
    */
  val values: IndexedSeq[E]
}
