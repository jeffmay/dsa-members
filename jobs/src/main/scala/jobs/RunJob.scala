package org.dsasf.members
package jobs

import zio._
import zio.console.Console

object RunJob extends App {

  override def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    for {
      _ <- ZIO.accessM[Console](_.get.putStrLn("Hello"))
    } yield ExitCode.success
  }
}
