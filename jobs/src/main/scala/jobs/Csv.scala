package org.dsasf.members
package jobs

import cats.Eq
import zio.stream._
import zio.{Has, IO, UIO, ZIO}

import scala.collection.immutable.ListMap
import scala.util.control.NoStackTrace

object Csv {

  trait Decode[A] extends DecodeWithHeaderCtx[A] {
    def decode(row: Row)(implicit ctx: Has[RowCtx]): IO[DecodeFailure, A]
    override def decodeWithHeader(row: Row)(implicit
      rowCtx: Has[HeaderCtx] with Has[RowCtx],
    ): IO[DecodeFailure, A] = decode(row)
  }

  final object Decode extends LowPriorityDecodeRowImplicits {

    def apply[A](implicit decoder: Decode[A]): Decode[A] = decoder

    def withContext[A](
      decoder: Has[RowCtx] ⇒ Row ⇒ IO[DecodeFailure, A],
    ): Decode[A] = new Decode[A] {
      final override def decode(row: Row)(implicit
        rowCtx: Has[RowCtx],
      ): IO[DecodeFailure, A] = decoder(rowCtx)(row)
    }

    def withoutContext[A](decoder: Row ⇒ IO[DecodeFailure, A]): Decode[A] =
      new Decode[A] {
        final override def decode(row: Row)(implicit
          rowCtx: Has[RowCtx],
        ): IO[DecodeFailure, A] = decoder(row)
      }
  }

  sealed private[Csv] trait LowPriorityDecodeRowImplicits {
    implicit def decodeRowWithoutHeader[A : Decode]: DecodeWithHeaderCtx[A] =
      new DecodeWithHeaderCtx[A] {
        final override def decodeWithHeader(row: Row)(
          implicit ctx: Has[HeaderCtx] with Has[RowCtx],
        ): IO[DecodeFailure, A] =
          Decode[A].decode(row)
      }
  }

  trait DecodeWithHeaderCtx[A] {
    def decodeWithHeader(row: Row)(implicit
      ctx: Has[HeaderCtx] with Has[RowCtx],
    ): IO[DecodeFailure, A]
  }

  final object DecodeWithHeaderCtx {

    def apply[A](implicit
      decoder: DecodeWithHeaderCtx[A],
    ): DecodeWithHeaderCtx[A] = decoder

    def fromEffect[A]: Builder[IO, A] = new Builder(identity)

    def fromEither[A]: Builder[Either, A] = new Builder(IO.fromEither(_))

    final class Builder[M[_, _], A](
      private val toIO: M[DecodeFailure, A] ⇒ IO[DecodeFailure, A],
    ) {

      def withContext(
        decoder: Has[HeaderCtx] with Has[RowCtx] ⇒ Row ⇒ M[DecodeFailure, A],
      ): DecodeWithHeaderCtx[A] = new DecodeWithHeaderCtx[A] {
        final override def decodeWithHeader(row: Row)(implicit
          ctx: Has[HeaderCtx] with Has[RowCtx],
        ): IO[DecodeFailure, A] =
          toIO(decoder(ctx)(row))
      }

      def withoutContext(
        decoder: Row ⇒ M[DecodeFailure, A],
      ): DecodeWithHeaderCtx[A] = new DecodeWithHeaderCtx[A] {
        final override def decodeWithHeader(row: Row)(implicit
          ctx: Has[HeaderCtx] with Has[RowCtx],
        ): IO[DecodeFailure, A] =
          toIO(decoder(row))
      }
    }
  }

  final case class HeaderCtx(columns: ListMap[String, Int])
  final object HeaderCtx {
    def apply(row: Row): HeaderCtx = apply(row.cells)
    def apply(row: Seq[String]): HeaderCtx =
      new HeaderCtx(ListMap.from(row.zipWithIndex))
  }

  final case class RowCtx(rowIndex: Long)

  sealed trait Failure extends Exception with NoStackTrace {
    def rowIndex: Long
  }

  sealed abstract class ParseFailure(
    override val rowIndex: Long,
    val reason: String,
    val cause: Option[Throwable],
  ) extends Exception(s"Parsing failure at row $rowIndex: $reason", cause.orNull)
    with Failure

  final case class InvalidSyntax(
    override val rowIndex: Long,
    syntaxError: String,
    override val cause: Option[Throwable],
  ) extends ParseFailure(
      rowIndex,
      s"Invalid CSV row syntax: $syntaxError",
      cause,
    )

  sealed abstract class DecodeFailure(
    override val rowIndex: Long,
    val reason: String,
  ) extends Exception(s"Decoding failure at row $rowIndex: $reason")
    with Failure

  final case class InvalidColumnName(
    override val rowIndex: Long,
    expectedColumnName: String,
  ) extends DecodeFailure(
      rowIndex,
      s"Expected a header column named '$expectedColumnName'",
    )

  final case class InvalidColumnIndex(
    override val rowIndex: Long,
    expectedColumnIndex: Int,
  ) extends DecodeFailure(
      rowIndex,
      s"Expected a column at index=$expectedColumnIndex",
    )

  sealed trait DecodeCellFailure extends DecodeFailure {
    def columnIndex: Int
  }

  final case class UnexpectedType(
    override val rowIndex: Long,
    columnIndex: Int,
    expectedType: String,
  ) extends DecodeFailure(
      rowIndex,
      s"Expected cell at column=$columnIndex (zero-indexed)" +
        s" to be of type '$expectedType'",
    )
    with DecodeCellFailure

  object Row {
    implicit val eq: Eq[Row] = Eq.instance(_.cells == _.cells)

    def fromArray(values: Array[String]): Row = new Row(values)
    def fromIterable(values: Iterable[String]): Row =
      new Row(values.toArray[String])
  }

  final class Row private (
    private val array: Array[String],
  ) extends AnyVal {

    def cells: IndexedSeq[String] = array

    def apply(idx: Int)(implicit
      ctx: Has[RowCtx],
    ): Either[InvalidColumnIndex, String] =
      get(idx).toRight(InvalidColumnIndex(ctx.get[RowCtx].rowIndex, idx))

    def apply(
      key: String,
    )(implicit
      ctx: Has[HeaderCtx] with Has[RowCtx],
    ): Either[DecodeFailure, String] =
      for {
        colIdx ← ctx.get[HeaderCtx].columns.get(key).toRight(InvalidColumnName(
          ctx.get[RowCtx].rowIndex,
          key,
        ))
        cell ← apply(colIdx)
      } yield cell

    def get(idx: Int): Option[String] =
      Option.when(array.isDefinedAt(idx))(array(idx))

  }

  def parse: Parse[Failure, Row] = ParseRows

  def parseAs[A](implicit decoder: Decode[A]): Parse[Failure, A] =
    new ParseDecodeNoHeader(decoder)

  def parseWithHeaderAs[A](implicit
    decoder: DecodeWithHeaderCtx[A],
  ): Parse[Failure, A] =
    new ParseDecodeWithHeader(decoder)

  private[Csv] def parseRow(line: String): Row = Row.fromArray(line.split(','))

  sealed trait Parse[+E, +A] extends Any {
    def fromLines(lines: UStream[String]): Stream[E, A]
  }

  final private object ParseRows extends Parse[Nothing, Row] {
    override def fromLines(lines: UStream[String]): Stream[Nothing, Row] =
      lines.map(parseRow)
  }

  sealed trait ParseWithDecoder[D[a] <: Decode[a], A]
    extends Any with Parse[Failure, A]

  final private class ParseDecodeNoHeader[A] private[Csv] (
    private val decoder: Decode[A],
  ) extends AnyVal
    with ParseWithDecoder[Decode, A] {
    override def fromLines(lines: UStream[String]): Stream[Failure, A] = {
      lines.zipWithIndex.mapM { case (line, idx) ⇒
        val row = parseRow(line)
        val ctx = Has(RowCtx(idx))
        decoder.decode(row)(ctx)
      }
    }
  }

  final private class ParseDecodeWithHeader[A] private[Csv] (
    private val decoder: DecodeWithHeaderCtx[A],
  ) extends AnyVal
    with ParseWithDecoder[DecodeWithHeaderCtx, A] {

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

    override def fromLines(lines: UStream[String]): Stream[Failure, A] = {
      val maybeResults = readHeader(lines).map {
        case Some((rows, header)) ⇒
          rows.zipWithIndex.map { case (line, idx) ⇒
            val row = parseRow(line)
            val ctx = Has.allOf(header, RowCtx(idx))
            decoder.decodeWithHeader(row)(ctx)
          }
        case None ⇒
          ZStream.empty
      }
      ZStream.fromEffectOption(maybeResults).flatten.mapM(identity)
    }

  }
}
