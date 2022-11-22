package org.dsasf.members
package pprint

object syntax {

  def productToStringPretty(p: Product): String = {
    p.productElementNames.zip(p.productIterator)
      .map { case (name, value) => s"$name=$value" }
      .mkString(p.productPrefix + "(", ", ", ")")
  }

  extension (p: Product) def toStringPretty: String = productToStringPretty(p)
}
