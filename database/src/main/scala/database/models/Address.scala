package org.dsasf.members
package database.models

final case class Address(
  line1: String,
  line2: String,
  city: String,
  state: String,
  zip: String,
)
