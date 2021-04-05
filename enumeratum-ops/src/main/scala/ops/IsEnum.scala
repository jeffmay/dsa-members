package enumeratum
package ops

import scala.annotation.implicitNotFound

@implicitNotFound("In order to use this feature, your enumeration object must extend IsEnum[${E}]")
abstract class IsEnum[E] {

  /** Defines how to convert enum values to string and back.
    *
    * Typically implemented with the case-insensitive `EnumCodec.fromEnum(this)`
    */
  implicit val codec: EnumCodec[E]

  /** All possible values of this enumeration.
    *
    * Typically implemented with enumeratum's `findValues` method
    */
  val values: IndexedSeq[E]

  /** The magic that allows summoning the [[IsEnum]] capabilities.
    *
    * Since this object is almost always the companion object of enumeration values
    * that it contains, it will always be in implicit scope.
    */
  implicit final def isEnum: IsEnum[E] = this
}

object IsEnum {
  @inline def apply[E : IsEnum]: IsEnum[E] = implicitly
}
