package org.dsasf.members
package api

import zio.json._
import zio.json.ast.Json

import java.time._
import scala.concurrent.duration._

final case class DeployInfo(deployedAt: OffsetDateTime) {

  def aliveSince(now: Instant): String = {
    val duration = (now.getEpochSecond - deployedAt.toEpochSecond).seconds
    val coarsest = duration.toCoarsest
    s"$coarsest ago"
  }

  def aliveSinceDefaultTimeZone(): String = aliveSince(Instant.now())
}

object DeployInfo {

  def toJsonString(deployInfo: DeployInfo, now: Instant): String = {
    val obj = BuildInfo
      .toJson
      .fromJson[Json.Obj]
      .fold(
        // this should never happen, so I am not even allowing it in the return type
        msg =>
          throw new IllegalStateException(
            s"sbt-buildinfo plugin produced invalid json: ${BuildInfo.toJson}\nError: $msg",
          ),
        identity,
      )
    obj
      .copy(fields =
        obj.fields ++
          Map(
            "deployTime" ->
              Json.Num(deployInfo.deployedAt.toInstant.toEpochMilli),
            "deployTimeAsString" -> Json.Str(deployInfo.deployedAt.toString),
            "aliveSince" -> Json.Str(deployInfo.aliveSince(now)),
          ),
      )
      .toJsonPretty
  }

}
