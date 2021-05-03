package org.dsasf.members
package database.migration

import cats.effect.{IO, IOApp}
import doobie.util.transactor.Transactor

trait DataMigration extends IOApp {

  def up(xa: Transactor[IO]): IO[Either[Throwable, MigrationResults]]

  def down(xa: Transactor[IO]): IO[Either[Throwable, MigrationResults]]
}

final case class MigrationResults(
  updates: Long = 0,
  errors: Seq[Throwable] = Seq(),
)
