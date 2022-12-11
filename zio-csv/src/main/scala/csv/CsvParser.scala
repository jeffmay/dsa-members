package zio
package csv

import zio.stream.{ZChannel, ZPipeline, ZSink, ZStream}
import com.github.tototoshi.csv.{
  CSVFormat,
  CSVParser,
  CSVReader,
  MalformedCSVException,
}

import java.io.{FileInputStream, IOException, InputStream}
import java.nio.charset.CharacterCodingException
import java.nio.file.Path

object CsvParser {

  /** Read each line of a file (handling multi-line strings) into CSV rows or
    * failures.
    *
    * If there is a problem reading the file (such as the file being deleted or
    * moved before the reader is finished), then a [[ReadingFailure]] (typically
    * a [[ReadingIOException]]) will be put in the failure channel of this
    * stream.
    *
    * If you would like to add the [[HeaderCtx]] to the rows, you can use
    * [[CSVDecoder.readHeaderInfo]] to read the first row as the header OR --
    * for better performance -- you can call [[fromFileWithHeader]].
    *
    * @note
    *   to convert this into a stream of [[Row]], you can either ignore the
    *   row-specific errors by calling `.collectRight` on the stream OR you can
    *   fail on the first row parsing failure by calling `.absolve`.
    *
    * @param path
    *   the path to the CSV file
    * @param format
    *   the [[CSVFormat]] to use when decoding this file
    * @return
    *   a stream of CSV [[Row]]s or [[RowParsingFailure]]s
    */
  def fromFile(
    path: Path,
    format: CSVFormat,
  ): ZStream[Any, ReadingFailure, Either[RowInvalidSyntax, Row[Any]]] = {
    val lines = readLinesFromFile(path, format)
    catchRowParsingFailures(lines)
  }

  /** Same as calling [[fromFile]] followed by [[CsvDecoder.readHeaderInfo]],
    * but faster, since it doesn't need to update all the rows after they have
    * been constructed.
    */
  def fromFileWithHeader(
    path: Path,
    format: CSVFormat,
  ): ZStream[Any, ReadingFailure, Either[RowInvalidSyntax, Row[HeaderCtx]]] = {
    val lines = readLinesFromFile(path, format)
    ZStream.unwrapScoped {
      for {
        peeled <- lines.peel(ZSink.head[Chunk[String]])
        (maybeHeaderCells, tail) = peeled
        headerCells <- ZIO.fromOption(maybeHeaderCells)
          .orElseFail(MissingHeaderFailure)
      } yield {
        val headerCtx = HeaderCtx(headerCells)
        catchRowParsingFailures(tail).map(_.map(_.addHeaderContext(headerCtx)))
      }
    }
  }

  private def readLinesFromFile(
    path: Path,
    format: CSVFormat,
  ): ZStream[Any, ReadingFailure, Chunk[String]] = {
    val acquireReader = ZIO.fromAutoCloseable {
      ZIO.attempt(CSVReader.open(path.toFile)(format))
    }
    ZStream.fromIteratorScoped(acquireReader.map(_.iterator))
      .refineOrDie {
        case ioe: IOException => ReadingIOException(ioe)
        case mfe: MalformedCSVException => ParsingException(mfe)
      }
      .map(Chunk.fromIterable)
  }

  // I can't make this a ZPipeline because pipelines don't handle recovery from previous parts of the stream.
  private def catchRowParsingFailures(
    rows: ZStream[Any, ReadingFailure, Chunk[String]],
  ): ZStream[Any, ReadingFailure, Either[RowInvalidSyntax, Row[Any]]] = {
    rows.either.zipWithIndex.mapZIO {
      case (Right(cells), index) =>
        ZIO.right(Row[Any](ZEnvironment(RowCtx(index, cells))))
      case (Left(parseEx @ ParsingException(csvEx)), index) =>
        ZIO.left(RowInvalidSyntax(index, csvEx.getMessage, Some(parseEx)))
      case (Left(f), _) => ZIO.fail(f)
    }
  }
}
