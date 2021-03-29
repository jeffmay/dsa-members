package org.dsasf.members
package api

import zhttp.http._
import zio.{Has, ZIO}
import zio.clock.Clock

// TODO: Provide this as a ZLayer?
object Routes {
  type Reqs = Has[DeployInfo] with Clock

  val monitorVersion: HttpApp[Reqs, HttpError] = Http.fromEffectFunction {
    case Method.GET → Root / "monitor" / "version" ⇒
      for {
        deployInfo ← ZIO.service[DeployInfo]
        clock ← ZIO.service[Clock.Service]
        now ← clock.instant
      } yield Response.jsonString(DeployInfo.toJsonString(deployInfo, now))
  }

  val all: HttpApp[Reqs, HttpError] = monitorVersion
}
