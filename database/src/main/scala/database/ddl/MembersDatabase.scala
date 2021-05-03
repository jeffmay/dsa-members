package org.dsasf.members
package database.ddl

import doobie.Update0
import doobie.implicits._

// TODO: Remove?
object MembersDatabase {

  val createDatabase: Update0 = sql"""
    CREATE DATABASE dsasf 
  """.update
}
