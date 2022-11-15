package org.dsasf.members
package api

import zhttp.service.Server
import zio._
import zio.config.getConfig

object AppServer {

  val run: ZIO[
    ServerConfig with Routes.Reqs,
    Throwable,
    Nothing,
  ] = {
    for {
      serverConfig <- getConfig[ServerConfig]
      _ <- Console.printLine(s"🚀 Server started on port ${serverConfig.port}")
      serverLoop <- Server.start(serverConfig.port, Routes.all)
    } yield serverLoop
  }

}
