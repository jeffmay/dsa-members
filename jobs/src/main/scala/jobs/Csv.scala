package org.dsasf.members
package jobs

import cats.{Eq, Monad}
import zio.Has.IsHas
import zio.stream._
import zio.{Has, IO, Tag, UIO, URIO, ZIO, ZLayer}

import java.time.{Instant, LocalDate, LocalDateTime, ZonedDateTime}
import scala.collection.immutable.ListMap
import scala.util.Try
import scala.util.control.NoStackTrace
import scala.util.matching.Regex

object Csv {

  trait RowDecoder[-R, A] {
    def decode(row: Row): RowDecoder.Result[R, A]
  }

  final object RowDecoder {
    type FromPositionOnly[A] = RowDecoder[Any, A]
    type FromHeaderCtx[A] = RowDecoder[Has[HeaderCtx], A]
    type MinCtx = Has[RowCtx]
    type Result[-R, A] = ZIO[R with MinCtx, DecodeFailure, A]

    @inline def apply[R, A](implicit
      decoder: RowDecoder[R, A],
    ): RowDecoder[R, A] = decoder
  }

  final case class HeaderCtx(columns: ListMap[String, Int])
  final object HeaderCtx {
    def apply(row: Row): HeaderCtx = apply(row.cells)
    def apply(row: Seq[String]): HeaderCtx =
      new HeaderCtx(ListMap.from(row.zipWithIndex))
  }

  final case class RowCtx(rowIndex: Long)

  sealed trait RowFailure extends Exception with NoStackTrace {
    def rowIndex: Long
  }

  sealed abstract class RowParsingFailure(
    override val rowIndex: Long,
    val reason: String,
    val cause: Option[Throwable],
  ) extends Exception(s"Parsing failure at row $rowIndex: $reason", cause.orNull)
    with RowFailure

  final case class RowInvalidSyntax(
    override val rowIndex: Long,
    syntaxError: String,
    override val cause: Option[Throwable],
  ) extends RowParsingFailure(
      rowIndex,
      s"Invalid CSV row syntax: $syntaxError",
      cause,
    )

  sealed abstract class DecodeFailure(
    override val rowIndex: Long,
    val reason: String,
    cause: Option[Throwable],
  ) extends Exception(
      s"Decoding failure at row $rowIndex: $reason",
      cause.orNull,
    )
    with RowFailure

  final case class InvalidColumnName(
    override val rowIndex: Long,
    expectedColumnName: String,
  ) extends DecodeFailure(
      rowIndex,
      s"Expected a header column named '$expectedColumnName'",
      None,
    )

  final case class InvalidColumnIndex(
    override val rowIndex: Long,
    expectedColumnIndex: Int,
  ) extends DecodeFailure(
      rowIndex,
      s"Expected a column at index=$expectedColumnIndex",
      None,
    )

  sealed trait CellDecodingFailure extends DecodeFailure {
    def columnIndex: Int
  }

  final object CellDecodingFailure {

    def fromExceptionDecodingAs[A : Tag](cause: Throwable): URIO[
      CellDecoder.MinCtx,
      CellDecodingTypedFailure[A],
    ] = {
      ZIO.services[RowCtx, CellCtx].map { case (row, cell) ⇒
        CellDecodingException[A](
          row.rowIndex,
          cell.columnIndex,
          cause,
        )
      }
    }
  }

  sealed trait CellDecodingTypedFailure[A] extends CellDecodingFailure {
    def expectedType: Tag[A]
    def withNewExpectedType[B : Tag]: CellDecodingTypedFailure[B]
  }

  final case class CellDecodingException[A : Tag](
    override val rowIndex: Long,
    columnIndex: Int,
    cause: Throwable,
  ) extends DecodeFailure(
      rowIndex,
      s"Expected cell at column index=$columnIndex to be of type ${Tag[A].tag}. Caused by:\n$cause",
      Some(cause),
    )
    with CellDecodingTypedFailure[A] {
    override val expectedType: Tag[A] = Tag[A]
    override def withNewExpectedType[B : Tag]: CellDecodingTypedFailure[B] =
      copy[B]()
  }

  sealed abstract class CellInvalidFormat[A : Tag](
    rowIndex: Long,
    columnIndex: Int,
    patternType: String,
    expectedPattern: String,
  ) extends DecodeFailure(
      rowIndex,
      s"Expected cell at column index=$columnIndex to match the following $patternType:\n$expectedPattern",
      None,
    )
    with CellDecodingTypedFailure[A] {
    override val expectedType: Tag[A] = Tag[A]
  }

  final case class CellInvalidUnmatchedRegex[A : Tag](
    override val rowIndex: Long,
    columnIndex: Int,
    expectedPattern: Regex,
  ) extends CellInvalidFormat[A](
      rowIndex,
      columnIndex,
      "regular expression",
      expectedPattern.pattern.pattern,
    ) {
    override def withNewExpectedType[B : Tag]: CellDecodingTypedFailure[B] =
      copy[B]()
  }

  final object Row {
    implicit val eq: Eq[Row] = Eq.instance(_.cells == _.cells)

    // specialized on array to avoid allocation and boxing
    // this is unsafe because it does not copy the mutable array
    @inline private[Csv] def unsafeFromArray(values: Array[String]): Row =
      new Row(values)

    def fromIterable(values: Iterable[String]): Row =
      new Row(values.toArray[String])
  }

  final class Row private (
    private val unsafeArray: Array[String],
  ) extends AnyVal {

    def cells: IndexedSeq[String] = unsafeArray

    def apply(idx: Int): Cell[Has[RowCtx]] = Cell.fromEffect {
      ZIO.fromOption {
        // build an option of our cell
        Option.when(unsafeArray.isDefinedAt(idx)) {
          CellCtx(idx, unsafeArray(idx))
        }
      }.flatMap { cell ⇒
        // grab the row context from the surrounding context
        ZIO.service[RowCtx].map { row ⇒
          Has.allOf(row, cell)
        }
      }.flatMapError { _ ⇒
        // we need the row context to produce our error as well
        ZIO.service[RowCtx].map { row ⇒
          InvalidColumnIndex(row.rowIndex, idx)
        }
      }
    }

    def apply(
      key: String,
    ): Cell[Has[HeaderCtx] with Has[RowCtx]] = Cell.fromEffect {
      for {
        // grab the header context so we can look up the column index by name
        header ← ZIO.service[HeaderCtx]
        // get the column index or fail
        colIdx ← IO.succeed(header.columns.get(key)).some.flatMapError { _ ⇒
          // we need the row context for our error message
          ZIO.service[RowCtx].map { row ⇒
            InvalidColumnName(row.rowIndex, key)
          }
        }
        // reuse the logic above to create our underlying
        cellCtx ← apply(colIdx).underlying
      } yield cellCtx
    }

  }

  final object Cell {

    def fromEffect[R](result: ZIO[R, DecodeFailure, Has[CellCtx]]): Cell[R] =
      new Cell(result)
  }

  final class Cell[R](
    private[Csv] val underlying: ZIO[R, DecodeFailure, Has[CellCtx]],
  ) extends AnyVal {

    def colIndex: ZIO[R, DecodeFailure, Int] =
      underlying.map(_.get[CellCtx].columnIndex)

    // this comes from the surrounding context and not the underlying ZIO, but it is here for convenience
    def rowIndex: URIO[Has[RowCtx], Long] = ZIO.service[RowCtx].map(_.rowIndex)

    def asString: ZIO[R, DecodeFailure, String] =
      underlying.map(_.get[CellCtx].content)

    def as[A](implicit
      decoder: CellDecoder[A],
    ): ZIO[R with Has[RowCtx], DecodeFailure, A] = {
      for {
        ctx ← underlying
        a ← CellDecoder[A]
          .decodeCell(ctx.get[CellCtx].content).provideSome[R with Has[RowCtx]] {
            // provide the resolved cell context as the environment for the decoder
            // the remaining context must come from outside the cell (i.e. the row context and any header context)
            _.union(ctx)
          }
      } yield a
    }
  }

  final case class CellCtx(columnIndex: Int, content: String)

  trait CellDecoder[A] {

    def decodeCell(cell: String): CellDecoder.Result[A]

    def mapSafe[B : Tag](fn: A ⇒ B): CellDecoder[B] =
      flatMapSafe(a ⇒ CellDecoder.const(fn(a)))

    def flatMapSafe[B : Tag](fn: A ⇒ CellDecoder[B]): CellDecoder[B] =
      CellDecoder.fromEffect { cell ⇒
        decodeCell(cell).flatMap { a ⇒
          ZIO.fromTry(Try(fn(a))).flatMap { decodeB ⇒
            decodeB.decodeCell(cell)
          }.flatMapError { ex ⇒
            for {
              row ← ZIO.service[RowCtx]
              cell ← ZIO.service[CellCtx]
            } yield CellDecodingException[B](
              row.rowIndex,
              cell.columnIndex,
              ex,
            )
          }
        }
      }
  }

  final object CellDecoder {
    type MinCtx = Has[RowCtx] with Has[CellCtx]
    type Result[A] = ZIO[MinCtx, CellDecodingFailure, A]

    @inline def apply[A](implicit
      decoder: CellDecoder[A],
    ): CellDecoder[A] = decoder

    implicit val string: CellDecoder[String] = IO.succeed(_)

    implicit val boolean: CellDecoder[Boolean] = {
      val validTrue = Set("true", "yes", "y", "on", "1")
      val validFalse = Set("false", "no", "n", "off", "0")
      val validOptions =
        (validTrue.toSeq ++ validFalse.toSeq).mkString("'", "', '", "'")
      fromTry { raw ⇒
        // skip obviously wrong values
        if (raw.length > "false".length) false
        else {
          val lowercase = raw.trim.toLowerCase
          if (validTrue.contains(lowercase)) true
          else if (validFalse.contains(lowercase)) false
          else throw new IllegalArgumentException(
            s"Unknown value '$raw'. Expected one of $validOptions",
          )
        }
      }
    }

    implicit val int: CellDecoder[Int] = fromTry(_.toInt)
    implicit val long: CellDecoder[Long] = fromTry(_.toLong)
    implicit val bigInt: CellDecoder[BigInt] = fromTry(BigInt(_))
    implicit val float: CellDecoder[Float] = fromTry(_.toFloat)
    implicit val double: CellDecoder[Double] = fromTry(_.toDouble)
    implicit val bigDecimal: CellDecoder[BigDecimal] = fromTry(BigDecimal(_))

    // TODO: Come up with more tolerant format options
    implicit val instant: CellDecoder[Instant] = fromTry(Instant.parse(_))
    implicit val localDate: CellDecoder[LocalDate] = fromTry(LocalDate.parse(_))
    implicit val localDateTime: CellDecoder[LocalDateTime] =
      fromTry(LocalDateTime.parse(_))
    implicit val zonedDateTime: CellDecoder[ZonedDateTime] =
      fromTry(ZonedDateTime.parse(_))

    def fromTry[A : Tag](convert: String ⇒ A): CellDecoder[A] = { str ⇒
      ZIO.fromTry(Try(convert(str)))
        .flatMapError { ex ⇒
          ZIO.services[RowCtx, CellCtx].map { case (row, cell) ⇒
            CellDecodingException[A](
              row.rowIndex,
              cell.columnIndex,
              ex,
            )
          }
        }
    }

    def const[A](value: A): CellDecoder[A] = { _ ⇒
      ZIO.succeed(value)
    }

    def fromEffect[A](convert: String ⇒ CellDecoder.Result[A]): CellDecoder[A] =
      convert(_)

    def fromEither[A](
      convert: String ⇒ Either[CellDecodingFailure, A],
    ): CellDecoder[A] = { str ⇒
      ZIO.fromEither(convert(str))
    }

    def matchesRegex(re: Regex): CellDecoder[String] = fromEffect { str ⇒
      ZIO.fromOption(Option.when(re.matches(str))(str))
        .flatMapError { _ ⇒
          for {
            row ← ZIO.service[RowCtx]
            cell ← ZIO.service[CellCtx]
          } yield CellInvalidUnmatchedRegex[String](
            row.rowIndex,
            cell.columnIndex,
            re,
          )
        }
    }

    def findAllMatches(re: Regex): CellDecoder[Iterable[Regex.Match]] =
      fromEffect { str ⇒
        val ll = LazyList.from(re.findAllMatchIn(str))
        ZIO.fromOption(Option.unless(ll.isEmpty)(ll))
          .flatMapError { _ ⇒
            for {
              row ← ZIO.service[RowCtx]
              cell ← ZIO.service[CellCtx]
            } yield CellInvalidUnmatchedRegex[Iterable[Regex.Match]](
              row.rowIndex,
              cell.columnIndex,
              re,
            )
          }
      }
  }

  def parse: Parse[RowFailure, Row] = ParseRows

  def parseAs[A](implicit
    decoder: RowDecoder.FromPositionOnly[A],
  ): Parse[RowFailure, A] =
    new ParseDecodeNoHeader(decoder)

  def parseWithHeaderAs[A](implicit
    decoder: RowDecoder.FromHeaderCtx[A],
  ): Parse[RowFailure, A] =
    new ParseDecodeWithHeader(decoder)

  private[Csv] def parseRow(line: String): Row = {
    // this is safe because the array is unused outside of this local scope
    Row.unsafeFromArray(line.split(','))
  }

  sealed trait Parse[+E, +A] extends Any {
    def fromLines(lines: UStream[String]): Stream[E, A]
  }

  final private object ParseRows extends Parse[Nothing, Row] {
    override def fromLines(lines: UStream[String]): Stream[Nothing, Row] =
      lines.map(parseRow)
  }

  sealed trait ParseWithDecoder[A] extends Any with Parse[RowFailure, A]

  final private class ParseDecodeNoHeader[A] private[Csv] (
    private val decoder: RowDecoder.FromPositionOnly[A],
  ) extends AnyVal
    with ParseWithDecoder[A] {
    override def fromLines(lines: UStream[String]): Stream[RowFailure, A] = {
      lines.zipWithIndex.mapM { case (line, idx) ⇒
        val row = parseRow(line)
        val ctx = Has(RowCtx(idx))
        decoder.decode(row).provide(ctx)
      }
    }
  }

  final private class ParseDecodeWithHeader[A] private[Csv] (
    private val decoder: RowDecoder.FromHeaderCtx[A],
  ) extends AnyVal
    with ParseWithDecoder[A] {

    private def readHeader(lines: UStream[String]): UIO[Option[(
      UStream[String],
      HeaderCtx,
    )]] = {
      lines.peel(ZSink.head).useNow.map { case (maybeHead, tail) ⇒
        maybeHead.map { firstLine ⇒
          val row = parseRow(firstLine)
          val header = HeaderCtx(row.cells)
          (tail, header)
        }
      }
    }

    override def fromLines(lines: UStream[String]): Stream[RowFailure, A] = {
      val maybeResults = readHeader(lines).map {
        case Some((rows, header)) ⇒
          rows.zipWithIndex.map { case (line, idx) ⇒
            val row = parseRow(line)
            val ctx = Has.allOf(header, RowCtx(idx))
            decoder.decode(row).provide(ctx)
          }
        case None ⇒
          ZStream.empty
      }
      ZStream.fromEffectOption(maybeResults).flatten.mapM(identity)
    }

  }
}
