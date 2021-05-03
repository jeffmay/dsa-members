package org.dsasf.members
package database.migration

import doobie._
import zio._
import zio.interop.catz._
import zio.logging._

abstract class DataMigration extends CatsApp {

  protected lazy val logger: ZLayer[ZEnv, Nothing, Logging] = Logging.console()

  implicit protected val logHandler: LogHandler = LogHandler { event =>
    runtime.unsafeRun(
      log.info(s"Executing SQL: ${event.sql}").provideLayer(logger),
    )
  }

  def upgrade(xa: Transactor[Task]): Task[MigrationResults]

  def downgrade(xa: Transactor[Task]): Task[MigrationResults]

  override def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    // A transactor that gets connections from java.sql.DriverManager and executes blocking operations
    // on an our synchronous EC. See the chapter on connection handling for more info.
    val xa = Transactor.fromDriverManager[Task](
      "org.postgresql.Driver", // driver classname
      "jdbc:postgresql:dsasf", // connect URL (driver-specific)
      "postgres", // user
      "u1h10HNjt1UH", // password
    )
    upgrade(xa).exitCode
  }
}

final case class MigrationResults(
  updates: Long = 0,
  errors: Seq[Throwable] = Seq(),
)
