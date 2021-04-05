package enumeratum
package ops

import scala.util.control.NoStackTrace

final case class NoSuchMember(
  validValues: Iterable[String],
  notFoundValue: String,
) extends NoSuchElementException
  with NoStackTrace {
  override val getMessage: String =
    s"$notFoundValue is not a member of Enum (${validValues.mkString(", ")})"
}
