package org.dsasf.members
package jobs

import jobs.national.ImportNationalMembership

import zio._
import zio.console.Console

object RunJob extends App {

  override def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    for {
      console ← ZIO.service[Console.Service]
//      _ ← console.putStrLn("Hello. What file do you want to process?")
//      filename ← console.getStrLn.orDie
      count ← ImportNationalMembership.fromCSV("tmp/dsasf-2021-03-21.csv").orDie
      _ ← console.putStrLn(s"Total records parsed: $count")
    } yield ExitCode.success
  }
}
