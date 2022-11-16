package zio
package csv

import stream.{ZPipeline, ZStream}

import com.github.tototoshi.csv.CSVFormat

import java.nio.file.Path

object CsvParser {

  // TODO: Support fromReader / FileReader from JavaScript

//  def fromFile(
//    path: Path,
//    format: CSVFormat,
//  ): ZStream[Any, ReadingFailure, Row[Any]] =
//    fromFileEither(path, format).absolve

  // TODO: Use ZChannel and ZPipeline

  def fromFile(
    path: Path,
    format: CSVFormat,
  ): ZStream[Any, ReadingFailure, Row[Any]] = {
    ZStream.fromPath(path) >>> ZPipeline.utf8Decode
    ???
  }

//  def fromFileEither(
//    path: Path,
//    format: CSVFormat,
//  ): ZStream[Any, ParsingFailure, Either[RowParsingFailure, Row[Any]]] = {
//    val parser = new CSVParser(format)
//    Files.readAllBytes(path)
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
