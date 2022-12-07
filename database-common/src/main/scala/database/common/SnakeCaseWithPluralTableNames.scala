package org.dsasf.members
package database.common

import io.getquill.{NamingStrategy, PluralizedTableNames, SnakeCase}

object SnakeCaseWithPluralTableNames extends SnakeCaseWithPluralTableNames
trait SnakeCaseWithPluralTableNames
  extends SnakeCase with PluralizedTableNames {
  override def default(s: String): String = SnakeCase.default(s)
  override def table(s: String): String = {
    // Ignore the pluralized table names implementation because its implementation is incorrect
    val sc = SnakeCase.table(s)
    if (sc.endsWith("s")) sc + "es"
    else sc + "s"
  }
}
