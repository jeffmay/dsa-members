package org.dsasf.members
package database.config

import zio.config._
import zio.config.magnolia._

final case class PostgresConfig(
  @describe("Postgres database hostname. " +
    "Use 127.0.0.1 if running this project from sbt locally. " +
    "Otherwise, it should be the Docker container name that is accessible from this container.")
  host: String,
  @describe("Postgres database name. Typically the name of the DSA chapter.")
  db: String,
  @describe("Postgres user name (default 'postgres', but using per-app user names is preferred).")
  user: String,
  @describe("Postgres user's password (please don't use 'CHANGEME').")
  password: String,
  @describe("Postgres database port (default 5432).")
  port: Int = 5432,
) {
  @inline def database: String = db
  @inline def username: String = user
  val jdbcUrl: String = s"jdbc:postgresql://$host:$port/$db"
}

object PostgresConfig {
  implicit val loader: ConfigDescriptor[PostgresConfig] =
    descriptor[PostgresConfig].describe("Postgres connection credentials")
}
