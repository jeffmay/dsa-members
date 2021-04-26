package zio
package csv

import csv.RowDecoder.Result
import stream.{ZSink, ZStream}

object CsvDecoder {

  /** Peels off the header row(s) and returns the [[HeaderCtx]] as well as
    * another stream containing the rest of the rows of data.
    */
  def readHeaderInfo[R, E >: RowFailure](
    rows: ZStream[R, E, Row[Any]],
  ): ZManaged[R, E, Option[(HeaderCtx, ZStream[R, E, Row[Has[HeaderCtx]]])]] = {
    rows.peel(ZSink.head).map { case (maybeHead, tail) =>
      maybeHead.map { firstRow =>
        val ctx = HeaderCtx(firstRow.cells)
        (ctx, tail.map(_.addToContext(ctx)))
      }
    }
  }

  def decodeRowsAs[A]: DecodeRowsAsOrFail[A] = new DecodeRowsAsOrFail

  def decodeRowsAsEitherFailureOr[A]: DecodeRowsAsEitherFailuresOr[A] =
    new DecodeRowsAsEitherFailuresOr
}

final class DecodeRowsAsOrFail[A](private val dummy: Boolean = true)
  extends AnyVal with DecodeRowsAs[RowFailure, A, A] {

  override protected def wrapResult[R](
    result: Result[R, A],
  ): ZIO[R, RowFailure, A] = result
}

final class DecodeRowsAsEitherFailuresOr[A](private val dummy: Boolean = true)
  extends AnyVal with DecodeRowsAs[Nothing, A, Either[RowFailure, A]] {

  override protected def wrapResult[R](
    result: Result[R, A],
  ): ZIO[R, Nothing, Either[RowFailure, A]] = result.either
}

sealed trait DecodeRowsAs[+E <: RowFailure, A, +T] extends Any {

  protected def wrapResult[R](
    result: RowDecoder.Result[R, A],
  ): ZIO[R, E, T]

  def usingPositionOnly[R, E1 >: E](
    rows: ZStream[R, E1, Row[Any]],
  )(implicit decoder: RowDecoder.FromPositionOnly[A]): ZStream[R, E1, T] = {
    rows.mapM { row =>
      wrapResult(decoder.decode(row))
    }
  }

  def usingHeaderInfo[R, E1 >: RowFailure](
    rows: ZStream[R, E1, Row[Has[HeaderCtx]]],
  )(implicit decoder: RowDecoder.FromHeaderInfo[A]): ZStream[R, E1, T] = {
    val readHeaderThenAllRows = CsvDecoder.readHeaderInfo(rows).map {
      case Some((header, dataRows)) =>
        dataRows.mapM { row =>
          val env = Has.allOf(header, row.rowContext)
          wrapResult(decoder.decode(row).provide(env))
        }
      case None =>
        ZStream.empty
    }
    // flatten the ZManaged ZStream into a single ZStream
    ZStream.managed(readHeaderThenAllRows).flatten
  }

  def providedHeader[R, E1 >: E](
    header: HeaderCtx,
    dataRows: ZStream[R, E1, Row[Any]],
  )(implicit
    decoder: RowDecoder.FromHeaderInfo[A],
  ): ZStream[R, E1, T] = {
    usingPositionOnly(dataRows)(decoder.withFixedHeader(header))
  }
}
