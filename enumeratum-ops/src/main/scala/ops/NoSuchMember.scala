package enumeratum
package ops

import scala.util.control.NoStackTrace

final case class NoSuchMember(
  enumName: String,
  validValues: Iterable[String],
  notFoundValue: String,
) extends NoSuchElementException
  with NoStackTrace {
  override val getMessage: String = {
    val enumDescription =
      if (validValues.isEmpty) s"empty enum, $enumName"
      else validValues.mkString(s"enum $enumName {'", "', '", "'}")
    s"'$notFoundValue' is not a member of $enumDescription"
  }
}
