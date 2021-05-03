package org.dsasf.members
package database.migration

import database.ddl._

import cats.effect.{ExitCode, IO}
import doobie.Transactor
import doobie.implicits._

object InitializeDatabase extends DataMigration {

  import cats.syntax.all._

  val tables: Seq[CreateTableOp with DropTableOp] = Seq(
    Users,
    NationalMembershipRecords,
    EmailAddresses,
    PhoneNumbers,
  )

  // Creates all the tables and returns the number of updates performed
  override def up(xa: Transactor[IO]): IO[Either[Throwable, MigrationResults]] =
    tables.traverse(_.createTable.update.run.transact(xa)).map { updates =>
      Right(MigrationResults(updates = updates.sum))
    }.handleErrorWith { ex =>
      IO.pure(Left(ex))
    }

  override def down(xa: Transactor[IO]): IO[Either[Throwable, MigrationResults]] =
    tables.reverse.traverse(_.dropTable.update.run.transact(xa)).map {
      updates =>
        Right(MigrationResults(updates = updates.sum))
    }.handleErrorWith { ex =>
      IO.pure(Left(ex))
    }

  override def run(args: List[String]): IO[ExitCode] = {
    // A transactor that gets connections from java.sql.DriverManager and executes blocking operations
    // on an our synchronous EC. See the chapter on connection handling for more info.
    val xa = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver", // driver classname
      "jdbc:postgresql:dsasf", // connect URL (driver-specific)
      "postgres", // user
      "u1h10HNjt1UH", // password
    )
    up(xa).as(ExitCode.Success)
  }
}
