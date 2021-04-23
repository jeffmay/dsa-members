package org.dsasf.members
package pprint

object syntax {

  def prettyToString(p: Product): String = {
    p.productElementNames.zip(p.productIterator)
      .map { case (name, value) => s"$name=$value" }
      .mkString(p.productPrefix + "(", ", ", ")")
  }

  implicit class PPrint(private val p: Product) extends AnyVal {
    def toStringPretty: String = prettyToString(p)
  }
}
