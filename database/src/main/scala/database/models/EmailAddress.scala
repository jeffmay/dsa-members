package org.dsasf.members
package database.models

final case class EmailAddress(
  username: String,
  domain: String,
) {
  val fullAddress: String = s"$username@$domain"
  override def toString: String = fullAddress
}
