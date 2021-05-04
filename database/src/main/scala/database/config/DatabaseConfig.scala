package org.dsasf.members
package database.config

import zio.config._
import zio.config.ConfigDescriptor._
import zio.system.System
import zio.{Has, ZLayer}

object DatabaseConfig {

  val fromEnv: ZLayer[System, ReadError[String], Has[PostgresConfig]] =
    ZConfig.fromSystemEnv(
      nested("postgres")(PostgresConfig.loader).mapKey {
        toSnakeCase.andThen(_.toUpperCase)
      },
      keyDelimiter = Some('_'),
    )
}
