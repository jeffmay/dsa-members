package org.dsasf.members
package api

import zio._
import zio.clock.Clock
import zio.config._

object Main extends App {

  override def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    // create the configuration layer
    // TODO: Move to separate config object?
    val configLayer = ZLayer.fromEffect {
      val cmdLineArgs = ConfigSource.fromCommandLineArgs(args)
      lazy val serverConfigFromCmdLine = ServerConfig
        .loader
        .mapKey(toKebabCase(_).toLowerCase)
        .from(cmdLineArgs)
      for {
        envVars ← ConfigSource.fromSystemEnv
        // Pull server configs from the command-line args first, then from env vars, then the defaults
        serverConfig ← IO.fromEither {
          read {
            lazy val serverConfigFromEnv = ServerConfig
              .loader
              .mapKey(toSnakeCase(_).toUpperCase)
              .from(envVars)
            serverConfigFromCmdLine orElse serverConfigFromEnv orElse
              ServerConfig.loader.default(ServerConfig(9000))
          }
        }
      } yield serverConfig
    }
    val deployInfoLayer = ZLayer.fromEffect {
      for {
        clock ← ZIO.service[Clock.Service]
        now ← clock.currentDateTime
      } yield DeployInfo(now)
    }
    // start the web server
    AppServer.run.provideCustomLayer(configLayer ++ deployInfoLayer).exitCode
  }

}
