package org.dsasf.members
package models

import eu.timepit.refined.api.Refined
import eu.timepit.refined.api.Refined.*
import eu.timepit.refined.refineV
import eu.timepit.refined.string.MatchesRegex

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

sealed trait EmailAddress extends Any {
  def fullAddress: String
  def username: String = fullAddress.take(fullAddress.indexOf('@'))
  def domain: String = fullAddress.drop(fullAddress.indexOf('@') + 1)

  override def equals(obj: Any): Boolean = fullAddress == obj
  override def toString: String = fullAddress
}

object EmailAddress {

  /** See https://stackoverflow.com/a/14075810/589581 */
  // language=RegExp
  inline val ValidPattern = "([-!#-'*+/-9=?A-Z^-~]+(\\.[-!#-'*+/-9=?A-Z^-~]+)*|\"(\\[]!#-[^-~ \\t]|(\\\\[\\t -~]))+\")@[0-9A-Za-z]([0-9A-Za-z-]{0,61}[0-9A-Za-z])?(\\.[0-9A-Za-z]([0-9A-Za-z-]{0,61}[0-9A-Za-z])?)+"
  type Valid = MatchesRegex[ValidPattern.type]

  inline def apply(fullAddress: String Refined Valid): EmailAddress = InputEmailAddress(fullAddress)

  inline def parse(address: String): Either[String, EmailAddress] = InputEmailAddress.parse(address)
}

final class InputEmailAddress private (override val fullAddress: String) extends AnyVal with EmailAddress {
  def toEssentialEmailAddress: EssentialEmailAddress = EssentialEmailAddress.fromInputEmailAddress(this)
}

object InputEmailAddress {
  def apply(fullAddress: String Refined EmailAddress.Valid): InputEmailAddress =
    new InputEmailAddress(fullAddress.value)

  inline def parse(address: String): Either[String, InputEmailAddress] =
    refineV[EmailAddress.Valid](address).map(InputEmailAddress(_))
}

final class EssentialEmailAddress private (override val fullAddress: String) extends AnyVal with EmailAddress
object EssentialEmailAddress {
  def fromInputEmailAddress(emailAddress: InputEmailAddress): EssentialEmailAddress = {
    new EssentialEmailAddress((emailAddress.username, emailAddress.domain) match {
      case (username, domain @ "gmail.com") => s"${username.replace(".", "")}@$domain"
      case _ => emailAddress.fullAddress
    })
  }
}
