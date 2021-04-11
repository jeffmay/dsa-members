package enumeratum
package ops

abstract class EnumCompanion[E] {

  /** Defines how to convert enum values to string and back.
    *
    * Typically implemented with the case-insensitive `EnumCodec.fromEnum(this)`
    */
  implicit val codec: EnumCodec[E]

  /** All possible values of this enumeration.
    *
    * Typically implemented with enumeratum's `findValues` method
    */
  def values: IndexedSeq[E]
}
