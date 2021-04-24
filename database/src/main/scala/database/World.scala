package org.dsasf.members
package database

import cats._
import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._

object World extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    // A transactor that gets connections from java.sql.DriverManager and executes blocking operations
    // on an our synchronous EC. See the chapter on connection handling for more info.
    val xa = Transactor.fromDriverManager[IO](
      "org.postgresql.Driver", // driver classname
      "jdbc:postgresql:world", // connect URL (driver-specific)
      "postgres", // user
      "u1h10HNjt1UH", // password
    )
    val program1 = 42.pure[ConnectionIO]
    val program2 = sql"select 42".query[Int].unique
    val program3: ConnectionIO[(Int, Double)] =
      for {
        a <- sql"select 42".query[Int].unique
        b <- sql"select random()".query[Double].unique
      } yield (a, b)
    val program4 = sql"select name from country"
      .query[String] // Query0[String]
      .to[List] // ConnectionIO[List[String]]
    for {
      rs <- program4.transact(xa)
      _ = rs.take(5).foreach(println)
//      _ = println(s"rs=$rs")
    } yield ExitCode.Success
  }
}
