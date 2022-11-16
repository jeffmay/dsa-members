package org.dsasf.members
package api

import zio._
import zio.config._

object Main extends ZIOAppDefault {

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    // create the configuration layer
    // TODO: Move to separate config object?
    val configLayer = ZLayer.fromZIO {
      for {
        args <- ZIO.service[ZIOAppArgs]
        // Pull server configs from the command-line args first, then from env vars, then the defaults
        serverConfig <- read {
          lazy val serverConfigFromCmdLine = ServerConfig
            .loader
            .mapKey(toKebabCase(_).toLowerCase)
            .from(ConfigSource.fromCommandLineArgs(args.getArgs.toList))
          lazy val serverConfigFromEnv = ServerConfig
            .loader
            .mapKey(toSnakeCase(_).toUpperCase)
            .from(ConfigSource.fromSystemEnv())
          serverConfigFromCmdLine orElse serverConfigFromEnv orElse
            ServerConfig.loader.default(ServerConfig(9000))
        }

      } yield serverConfig
    }
    val deployInfoLayer = ZLayer.fromZIO {
      for {
        now <- Clock.currentDateTime
      } yield DeployInfo(now)
    }
    // start the web server
    AppServer.run.provideSome[ZIOAppArgs](
      configLayer,
      deployInfoLayer,
    )
  }

}
