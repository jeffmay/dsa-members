package org.dsasf.members
package models

type Name = Name.NameValue
object Name:
  opaque type NameValue = String
  inline def apply(inline name: String): NameValue = name
  extension (inline name: NameValue) inline def value: String = name
