package zio
package csv

import stream.{ZSink, ZStream}

import com.github.tototoshi.csv.{CSVFormat, CSVParser}

import java.io.Reader
import scala.util.control.NonFatal

object CsvParser {

  // TODO: Replace these with a sink / channel
  //       and a method to convert from a file to said stream that does not load everything into memory

  // TODO: Support fromReader / FileReader from JavaScript

//  def fromReader(reader: Reader): ZIO[Scope, ReadingFailure, Row[Any]]

//  def fromFile(
//    path: Path,
//    format: CSVFormat,
//  ): ZStream[Any, ReadingFailure, Row[Any]] =
//    fromFileEither(path, format).absolve

  // TODO: Use ZChannel

//  def fromFileEither(
//    path: Path,
//    format: CSVFormat,
//  ): ZStream[Any, ParsingFailure, Either[RowParsingFailure, Row[Any]]] = {
//    val parser = new CSVParser(format)
//    ZStream.fromPath(path)
//    for {
//      c <- ZStream.fromPath(path)
//      stillOpen <- c
//    } yield ()
////    val x = ZStream.fromPath(path)
//    val x = Files.readAllBytes(path)
//      .refineOrDie {
//        case NonFatal(ex) => ParsingException(ex)
//      }
//      .zipWithIndex
//      .map { case (line, index) =>
//        parser.parseLine(line).map(Row.noHeaderCtx(index, _)).toRight {
//          RowInvalidSyntax(index, s"Malformed Input: $line", None)
//        }
//      }
//  }
}
