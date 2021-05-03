package org.dsasf.members
package database.ddl

import doobie._

sealed trait TableOp

trait CreateTableOp extends TableOp {
  val createTable: Fragment
}

trait DropTableOp extends TableOp {
  val dropTable: Fragment
}
