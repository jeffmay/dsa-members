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
  ): ZStream[Blocking, ReadingFailure, Row] = {
    val parser = new CSVParser(format)
    ZStream.fromFile(path)
      .transduce(ZTransducer.utf8Decode >>> ZTransducer.splitLines)
      .refineOrDie {
        case NonFatal(ex) ⇒ ParsingException(ex)
      }
      .zipWithIndex
      .mapM { case (line, index) ⇒
        ZIO.fromEither {
          parser.parseLine(line).map(Row.fromIterable).toRight {
            RowInvalidSyntax(index, s"Malformed Input: $line", None)
          }
        }
      }
  }
}
