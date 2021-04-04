package org.dsasf.members
package jobs.national

import jobs.Csv
import jobs.Csv.RowFailure

import zio.{Chunk, Has, ZIO, ZManaged}
import zio.blocking.Blocking
import zio.console.Console
import zio.nio.channels.{AsynchronousFileChannel, FileChannel}
import zio.nio.core.charset.Charset
import zio.nio.file.Files
import zio.nio.core.file.Path
import zio.stream.{UStream, ZSink, ZStream}

import java.net.URI
import java.nio.file.{Paths, StandardOpenOption}
import scala.annotation.tailrec
import scala.io.Codec

object ImportNationalMembership {

  final case class ImportResults(
    successes: Long = 0,
    failures: Long = 0,
  ) {

    def recordSuccess(record: CsvRecord): ImportResults =
      copy(successes = this.successes + 1)

    def recordFailure(failure: RowFailure): ImportResults =
      copy(successes = this.failures + 1)
  }

  final case class ImportFailed(
    results: ImportResults,
  )

  def fromCSV(filename: String): ZIO[
    Console with Blocking,
    RowFailure,
    Long,
  ] = {
    for {
//      blocking ← ZIO.service[Blocking.Service]
      path ← ZIO(Path(filename)).orDie
//      count ← ZIO.open(path, StandardOpenOption.READ).use {
//        fileChan ⇒
//          val byteStream = ZStream.repeatEffectChunkOption {
//            fileChan.readChunk(1000).asSomeError.flatMap { chunk ⇒
//              if (chunk.isEmpty) ZIO.fail(None) else ZIO.succeed(chunk)
//            }
//          }
//          val charStream = byteStream
//            .transduce(
//              Charset.fromJava(Codec.UTF8.charSet).newDecoder.transducer(),
//            )
//          for {
//            x ← parseAll(inStream).run(ZSink.count)
//          } yield x
//      }
      lines ← Files.readAllLines(path).orDie
      count ← parseAll(ZStream.fromIterable(lines)).runCount
    } yield count
  }

  // TODO: Figure out what to do with this attempt to stream a file into lines
//  def splitIntoLines(charStream: UStream[Char]): UStream[String] = {
//    // Helper method to grab characters until a newline is reached, prepending it to the list
//    // If the remaining chunk does not end in a newline, then
//    @tailrec def readLinesReversed(
//      lineReversed: List[String],
//      remaining: Chunk[Char],
//    ): (List[String], Chunk[Char]) = {
//      val (chunkToNewline, tailChunk) = remaining.splitWhere(_ == '\n')
//      // either the chunk was split on the first newline encountered (thus the tail is a newline char)
//      // or the tail chunk is empty because no newline was found
//      assert(
//        chunkToNewline.lastOption.contains('\n') || tailChunk.isEmpty,
//        s"How does splitWhere work? [chunkToNewLine='${chunkToNewline.mkString}', tailChunk='${tailChunk.mkString}']'",
//      )
//      val (completeLines, remainingChunk) =
//        if (chunkToNewline.lastOption.contains('\n'))
//          (chunkToNewline.mkString :: lineReversed, tailChunk)
//        else (lineReversed, chunkToNewline ++ tailChunk)
//
//      if (remainingChunk.isEmpty) (completeLines, remainingChunk)
//      else {
//        readLinesReversed(completeLines, remainingChunk)
//      }
//    }
//
//    def peelLines(remainingCharStream: UStream[Char]): ZManaged[
//      Any,
//      Nothing,
//      List[String],
//    ] = {
//      val managedChunk = charStream.peel {
//        ZSink.foldChunks((List.empty[String], Chunk[Char]()))(
//          _._1.tail.nonEmpty,
//        ) {
//          case ((linesReversed, remainder), nextChunk) ⇒
//            readLinesReversed(linesReversed, remainder ++ nextChunk)
//        }
//      }
//      managedChunk.use { case ((reversedLines, incompleteLine), streamTail) ⇒
//        val remaining = ZStream.fromChunk(incompleteLine) ++ streamTail
//        if (remaining)
//          reversedLines.reverse
//      }
//    }
//  }

  // TODO: Handle escape Strings
  def parseAll(lines: UStream[String]) = {
    val allRecordsOrError = Csv.parseWithHeaderAs[CsvRecord].fromLines(lines)
    // TODO: Configure parallelism here
    val y = allRecordsOrError.scan(ImportResults()) {
      (res, record) ⇒ res.recordSuccess(record)
    }
    //    val x = allRecordsOrErrors.mapMParUnordered(10) { record ⇒ }
    //    x
    //    y.re
    y
  }
}
