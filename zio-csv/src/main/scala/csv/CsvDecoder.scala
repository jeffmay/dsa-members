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
  ): ZIO[R with Scope, E, Option[ZStream[R, E, Row[HeaderCtx]]]] = {
    rows.peel(ZSink.head[Row[Any]]).map { case (maybeHead, tail) =>
      maybeHead.map { firstRow =>
        val ctx = HeaderCtx(firstRow.context.cellContents)
        tail.map(_.mapContext { c =>
          c.copy(
            // remove the header row from the row index
            rowIndex = c.rowIndex - 1,
          )
        }.addHeaderContext(ctx))
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
    result: Result[A],
  ): ZIO[R, RowFailure, A] = result
}

final class DecodeRowsAsEitherFailuresOr[A](private val dummy: Boolean = true)
  extends AnyVal with DecodeRowsAs[Nothing, A, Either[DecodingFailure, A]] {

  override protected def wrapResult[R](
    result: Result[A],
  ): ZIO[R, Nothing, Either[DecodingFailure, A]] = result.either
}

sealed trait DecodeRowsAs[+E <: RowFailure, A, +T] extends Any {

  protected def wrapResult[R](
    result: RowDecoder.Result[A],
  ): ZIO[R, E, T]

  def apply[R, E1 >: E, H](
    rows: ZStream[R, E1, Row[H]],
  )(using decoder: RowDecoder[H, A]): ZStream[R, E1, T] = {
    rows.mapZIO { row =>
      wrapResult(decoder.decode(row))
    }
  }

  // TODO: Is this needed? I think contravariance should handle this case.
  def usingPositionOnly[R, E1 >: E](
    rows: ZStream[R, E1, Row[Any]],
  )(implicit decoder: RowDecoder.FromPositionOnly[A]): ZStream[R, E1, T] = {
    rows.mapZIO { row =>
      wrapResult(decoder.decode(row))
    }
  }

  def usingHeaderInfo[R, E1 >: RowFailure](
    rows: ZStream[R, E1, Row[Any]],
  )(implicit
    decoder: RowDecoder.FromHeaderInfo[A],
  ): ZStream[R, E1, T] = {
    val readHeaderThenAllRows = CsvDecoder.readHeaderInfo(rows).map {
      case Some(rows) =>
        rows.mapZIO { row =>
          wrapResult(decoder.decode(row))
        }
      case None =>
        ZStream.fail(MissingHeaderFailure)
    }
    // flatten the ZManaged ZStream into a single ZStream
    ZStream.unwrapScoped[R](readHeaderThenAllRows)
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
