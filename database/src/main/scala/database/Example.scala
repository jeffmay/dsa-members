package org.dsasf.members
package database

import eu.timepit.refined.api.Refined
import models.*
import io.getquill.*
import io.getquill.autoQuote
import sourcecode.Text.generate

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext.Implicits.global
import io.getquill.util.LoadConfig
import eu.timepit.refined.refineV
import eu.timepit.refined.auto.*
import queries.{SnakeCaseWithPluralTableNames, UserQueries}
import zio.{ZIO, ZIOAppDefault}

import java.util.UUID

object Example extends ZIOAppDefault {

  override def run: ZIO[Any, Any, Any] = {
    val config = LoadConfig("dsasf")
    val ctx = new PostgresJAsyncContext(SnakeCaseWithPluralTableNames, config)
    val users = UserQueries(ctx)
    val sampleUser = User(UserId(UUID.randomUUID()), Name("Jeff May"), EmailAddress("jeff.n.may@gmail.com"))
    for {
      _ <- ZIO.fromFuture(_ => ctx.run(users.create(sampleUser)))
      foundUsers <- ZIO.fromFuture(_ => ctx.run(users.findAll))
      _ <- ZIO.logInfo(s"Found Users:\n  ${foundUsers.mkString("\n  ")}")
    } yield ()
  }
}
