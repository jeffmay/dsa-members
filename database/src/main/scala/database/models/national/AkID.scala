package org.dsasf.members
package database.models.national

opaque type AkID = String
object AkID {
  def apply(value: String): AkID = value
}

extension (akId: AkID) inline def value: String = akId
