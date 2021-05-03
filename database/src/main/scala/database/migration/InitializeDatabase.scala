package org.dsasf.members
package database.migration

import database.ddl._

import cats.syntax.all._
import doobie.Transactor
import doobie.implicits._
import zio.Task
import zio.interop.catz._

object InitializeDatabase extends DataMigration {

  val tables: Seq[CreateTableOp with DropTableOp] = Seq(
    Users,
    NationalMembershipRecords,
    EmailAddresses,
    PhoneNumbers,
  )

  // Creates all the tables and returns the number of updates performed
  override def up(xa: Transactor[Task]): Task[MigrationResults] = {
    tables.traverse(_.createTable.update.run.transact(xa)).map {
      updates => MigrationResults(updates = updates.sum)
    }
  }

  override def down(xa: Transactor[Task]): Task[MigrationResults] =
    tables.reverse.traverse(_.dropTable.update.run.transact(xa)).map {
      updates => MigrationResults(updates = updates.sum)
    }
}
