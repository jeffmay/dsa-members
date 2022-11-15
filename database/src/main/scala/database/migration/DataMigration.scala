package org.dsasf.members
package database.migration

import database.config.{DatabaseConfig, PostgresConfig}

import doobie._
import zio._
import zio.interop.catz._
import zio.logging._

abstract class DataMigration extends CatsApp {
  final type MEnv = DataMigration.MEnv
  final type MTask[+A] = ZIO[MEnv, Throwable, A]

//  protected def hasLogging: ZLayer[ZEnv, Nothing, Logging] =
//    DataMigration.defaultLogging
//
//  protected def hasLogHandler: ULayer[LogHandler] =
//    DataMigration.defaultLogHandler(runtime)

  def upgrade(xa: Transactor[Task])(implicit
    lh: LogHandler,
  ): MTask[MigrationResults]

  def downgrade(xa: Transactor[Task])(implicit
    lh: LogHandler,
  ): MTask[MigrationResults]

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    // Create migration
//    val migration = for {
//      conf <- ZIO.service[PostgresConfig]
//      _ <- ZIO.logInfo(s"Connecting to ${conf.jdbcUrl}")
//      xa = Transactor.fromDriverManager[Task](
//        "org.postgresql.Driver",
//        conf.jdbcUrl,
//        conf.username,
//        conf.password,
//      )
//      lh <- ZIO.service[LogHandler]
//      results <- upgrade(xa)(lh)
//    } yield results
//    // Run the migration with the provided environment
//    migration.provide(
//      DatabaseConfig.fromEnv,
//      hasLogging,
//      hasLogHandler,
//    )
    ZIO.logInfo(s"Connecting to database...")
  }
}

object DataMigration {
  type MEnv = Any

//  val defaultLogging: ZLayer[ZEnv, Nothing, Logging] = Logging.console()

//  def defaultLogHandler(runtime: Runtime[Any]): ULayer[LogHandler] = {
//    ZLayer.fromZIO {
//      ZIO.loggersWith { loggers =>
//        LogHandler { event =>
//          runtime.unsafeRun {
//            log.info(s"Executing SQL: ${event.sql}").provide(logging)
//          }
//        }
//      }
//    }
//  }
}

final case class MigrationResults(
  updates: Long = 0,
  errors: Seq[Throwable] = Seq(),
)
