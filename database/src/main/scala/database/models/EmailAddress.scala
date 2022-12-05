package org.dsasf.members
package database.models

import eu.timepit.refined.api.Refined
import eu.timepit.refined.api.Refined.*
import eu.timepit.refined.refineV
import eu.timepit.refined.string.MatchesRegex
import io.getquill.MappedEncoding
import org.dsasf.members.stringutil.Diff
import zio.Chunk

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
  // TODO: Use the official regex for emails
  // language=RegExp
  final val ValidPattern = "[^@]+@[a-z.-]+"
  type Valid = MatchesRegex[ValidPattern.type]

  def apply(fullAddress: String Refined Valid): EmailAddress = InputEmailAddress(fullAddress)
  def essential(fullAddress: String Refined Valid): EssentialEmailAddress = InputEmailAddress(fullAddress).toEssentialEmailAddress

  given MappedEncoding[EmailAddress, String] = MappedEncoding(_.fullAddress)
  given MappedEncoding[String, EmailAddress] = MappedEncoding(
    InputEmailAddress.parse(_).fold(msg => throw new IllegalStateException(msg), identity)
  )
}

final class InputEmailAddress private (override val fullAddress: String) extends AnyVal with EmailAddress {
  def toEssentialEmailAddress: EssentialEmailAddress = EssentialEmailAddress.fromInputEmailAddress(this)
}

object InputEmailAddress {
  def apply(fullAddress: String Refined EmailAddress.Valid): InputEmailAddress =
    new InputEmailAddress(fullAddress.value)

  def parse(address: String): Either[String, InputEmailAddress] =
    refineV[EmailAddress.Valid](address).map(InputEmailAddress(_))

  given MappedEncoding[InputEmailAddress, String] = MappedEncoding(_.fullAddress)
  given MappedEncoding[String, InputEmailAddress] = MappedEncoding(
    parse(_).fold(msg => throw new IllegalStateException(msg), identity)
  )
}

final class EssentialEmailAddress private (override val fullAddress: String) extends AnyVal with EmailAddress
object EssentialEmailAddress {
  def fromInputEmailAddress(emailAddress: InputEmailAddress): EssentialEmailAddress = {
    new EssentialEmailAddress((emailAddress.username, emailAddress.domain) match {
      case (username, domain @ "gmail.com") => s"${username.replace(".", "")}@$domain"
      case _ => emailAddress.fullAddress
    })
  }

  given MappedEncoding[EssentialEmailAddress, String] = MappedEncoding(_.fullAddress)

  given MappedEncoding[String, EssentialEmailAddress] = MappedEncoding(
    original => InputEmailAddress.parse(original).flatMap { email =>
      val essentialEmail = email.toEssentialEmailAddress
      val diff = Diff(essentialEmail.fullAddress, original)
      diff.firstDiff.fold(Right(essentialEmail)) { case (idx, leftDiff, rightDiff) =>
        Left(
          s"EssentialEmailAddress in database did not match computed address starting at index=$idx:\n" +
            s"  Expected: '$leftDiff'\n" +
            s"  Observed: '$rightDiff'"
        )
      }
    }.fold(msg => throw new IllegalStateException(msg), identity)
  )
}
