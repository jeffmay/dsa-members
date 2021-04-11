package org.dsasf.members
package jobs

import jobs.national.ImportNationalMembership

import zio._
import zio.console.Console
import zio.csv.CsvFormat

import java.nio.file.Paths

object RunJob extends App {

  override def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    for {
      console ← ZIO.service[Console.Service]
//      _ ← console.putStrLn("Hello. What file do you want to process?")
//      filename ← console.getStrLn.orDie
      count ← ImportNationalMembership.fromCsvFile(
        Paths.get(
          "tmp",
          "dsasf-2021-03-21.csv",
        ),
        CsvFormat.Default,
      )
      _ ← console.putStrLn(
        s"Total successes: ${count.successes}, failures: ${count.failures}",
      )
    } yield ExitCode.success
  }
}
