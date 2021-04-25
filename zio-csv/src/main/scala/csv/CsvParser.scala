package zio
package csv

import blocking.Blocking
import stream.{ZStream, ZTransducer}

import com.github.tototoshi.csv.{CSVFormat, CSVParser}

import java.nio.file.Path
import scala.util.control.NonFatal

object CsvParser {

  def fromFile(
    path: Path,
    format: CSVFormat,
  ): ZStream[Blocking, ReadingFailure, Row] =
    fromFileEither(path, format).absolve

  def fromFileEither(
    path: Path,
    format: CSVFormat,
  ): ZStream[Blocking, ParsingFailure, Either[RowParsingFailure, Row]] = {
    val parser = new CSVParser(format)
    ZStream.fromFile(path)
      .transduce(ZTransducer.utf8Decode >>> ZTransducer.splitLines)
      .refineOrDie {
        case NonFatal(ex) => ParsingException(ex)
      }
      .zipWithIndex
      .map { case (line, index) =>
        parser.parseLine(line).map(Row(index, _)).toRight {
          RowInvalidSyntax(index, s"Malformed Input: $line", None)
        }
      }
  }
}
