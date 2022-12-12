package zio.util

import scala.annotation.implicitNotFound
import scala.compiletime.constValueTuple
import scala.deriving.Mirror

@implicitNotFound(
  "No SubTypeDiscriminator implicit or given for ${E} and the compiler could not find a Sum type Mirror." +
    "You can fix this by defining a given SubTypeDiscriminator[${E}] in the companion object of ${E} " +
    "using one of SubTypeDiscriminator.{fromField, fromFunction, fromSubTypeNames}."
)
trait SubTypeDiscriminator[-E]:
  extension (value: E) def discriminator: String

object SubTypeDiscriminator:

  inline given fromSubTypeNames[E](using m: Mirror.SumOf[E]): SubTypeDiscriminator[E] =
    val subTypeNames = constValueTuple[m.MirroredElemLabels].productIterator.map(_.toString).toArray
    enumCase => subTypeNames(m.ordinal(enumCase))

  // TODO: Capture the source of the discriminator in an enum (whether field or class name)

  def fromField[E](name: String, fn: E => String): SubTypeDiscriminator[E] = enumCase => fn(enumCase)

  def fromFunction[E](fn: E => String): SubTypeDiscriminator[E] = enumCase => fn(enumCase)
