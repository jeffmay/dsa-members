package org.dsasf.members
package database.migration

import database.ddl._

import cats.syntax.all._
import doobie.Transactor
import doobie.implicits._
import doobie.util.log.LogHandler
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
  override def upgrade(xa: Transactor[Task])(implicit
    lh: LogHandler,
  ): MTask[MigrationResults] = {
    tables.traverse(_.createTable.update.run.transact(xa)).map {
      updates => MigrationResults(updates = updates.sum)
    }
  }

  override def downgrade(xa: Transactor[Task])(implicit
    lh: LogHandler,
  ): MTask[MigrationResults] =
    tables.reverse.traverse(_.dropTable.update.run.transact(xa)).map {
      updates => MigrationResults(updates = updates.sum)
    }
}
