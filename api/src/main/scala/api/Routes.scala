package org.dsasf.members
package api

import zhttp.http._
import zio._

// TODO: Provide this as a ZLayer?
object Routes {
  type Reqs = DeployInfo

  val all: HttpApp[Reqs, Throwable] = Http.collectZIO {
    case Method.GET -> !! / "monitor" / "version" =>
      for {
        deployInfo <- ZIO.service[DeployInfo]
        now <- Clock.instant
      } yield Response.json(DeployInfo.toJsonString(deployInfo, now))
  }
}
