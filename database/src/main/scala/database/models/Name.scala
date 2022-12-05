package org.dsasf.members
package database.models

import io.getquill.MappedEncoding
import io.getquill.generic.GenericDecoder

final case class Name(value: String) extends AnyVal {
  override def toString: String = value
}
object Name {
  given MappedEncoding[Name, String] = MappedEncoding(_.value)
  given MappedEncoding[String, Name] = MappedEncoding(Name(_))
}
