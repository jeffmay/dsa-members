package org.dsasf.members
package api

import zhttp.service.Server
import zio._
import zio.config.getConfig
import zio.console.Console

object AppServer {

  val run: ZIO[
    Has[ServerConfig] with Console with Routes.Reqs,
    Throwable,
    Nothing,
  ] = {
    for {
      serverConfig <- getConfig[ServerConfig]
      _ <- console.putStrLn(s"🚀 Server started on port ${serverConfig.port}")
      serverLoop <- Server.start(serverConfig.port, Routes.all)
    } yield serverLoop
  }

}
