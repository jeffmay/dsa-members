package org.dsasf.members
package database

import database.common.DefaultNamingStrategy
import database.queries.UserQueries
import models.*

import eu.timepit.refined.api.Refined
import io.getquill.*
import io.getquill.util.LoadConfig
import sourcecode.Text.generate
import zio.{ZIO, ZIOAppDefault}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

object Example extends ZIOAppDefault {

  override def run: ZIO[Any, Any, Any] = {
    val config = LoadConfig("dsasf")
    val ctx = new DatabaseContextWithEncoders(config)
    import ctx.{*, given}
    val users = UserQueries(ctx)
    for {
      sampleEmail <- ZIO.fromEither(EmailAddress.parse("jeff.n.may@gmail.com"))
      sampleUser = User(
        UserId(UUID.randomUUID()),
        Name("Jeff May"),
        sampleEmail,
      )
      _ <- ZIO.fromFuture(_ => ctx.run(users.create(lift(sampleUser))))
      foundUsers <- ZIO.fromFuture(_ => ctx.run(users.findAll))
      _ <- ZIO.logInfo(s"Found Users:\n  ${foundUsers.mkString("\n  ")}")
    } yield ()
  }
}
