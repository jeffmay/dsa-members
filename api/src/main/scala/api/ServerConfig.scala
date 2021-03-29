package org.dsasf.members
package api

import zio.config._
import zio.config.magnolia._

final case class ServerConfig(
  @describe("the port on which the app listens for requests")
  port: Int,
)

object ServerConfig {
  implicit val loader: ConfigDescriptor[ServerConfig] = descriptor[ServerConfig]
    .describe("server-level configuration")
}
