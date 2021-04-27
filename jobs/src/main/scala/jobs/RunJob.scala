package org.dsasf.members
package jobs

import org.dsasf.members.jobs.national.ImportNationalMembership
import zio._
import zio.csv.CsvFormat
import zio.stream.{ZStream, ZTransducer}

import java.nio.file.{Path, Paths}

object RunJob extends App {

  private val sampleFile: Path = Paths.get("tmp", "dsasf-2021-03-21.csv")

  override def run(args: List[String]): URIO[ZEnv, ExitCode] = {
//    val linesOfFile = ZStream.fromFile(sampleFile).transduce(
//      ZTransducer.utfDecode >>> ZTransducer.splitLines,
//    )
//    val printAll = for ((line, idx) <- linesOfFile.zipWithIndex)
//      console.putStrLn(s"Line ${idx + 1}: $line")

//    for {
//      _ <- printAll.orDie
//    } yield ExitCode.success
    for {
//      console <- ZIO.service[Console.Service]
//      _ <- console.putStrLn("Hello. What file do you want to process?")
//      filename <- console.getStrLn.orDie
      rs <- ImportNationalMembership.fromCsvFile(
        Paths.get(
          "tmp",
          "dsasf-2021-03-21.csv",
        ),
        CsvFormat.Default,
      )
      _ <- console.putStr(
        s"Total successes: ${rs.successCount}, failures: ${rs.failureCount}\n${rs.allFailures.mkString("\n")}\n",
      )
    } yield ExitCode.success
  }
}
