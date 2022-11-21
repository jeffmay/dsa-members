package org.dsasf.members
package database.models

opaque type Name = String
object Name {
  def apply(value: String): Name = value
}

extension (name: Name) inline def value: String = name
