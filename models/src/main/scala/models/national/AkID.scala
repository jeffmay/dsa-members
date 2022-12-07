package org.dsasf.members
package models.national

type AkID = AkID.Type
object AkID:
  opaque type Type = String
  inline def apply(inline value: String): AkID = value
  extension (id: Type) inline def value: String = id
