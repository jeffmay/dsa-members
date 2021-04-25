package zio
package csv

import stream.{ZSink, ZStream}

object CsvDecoder {

  /** Peels off the header row(s) and returns the [[HeaderCtx]] as well as
    * another stream containing the rest of the rows of data.
    */
  def readHeaderInfo[R, E >: RowFailure](
    rows: ZStream[R, E, Row],
  ): ZManaged[R, E, Option[(HeaderCtx, ZStream[R, E, Row])]] = {
    rows.peel(ZSink.head[Row]).map { case (maybeHead, tail) =>
      maybeHead.map { firstRow =>
        val header = HeaderCtx.fromRow(firstRow)
        (header, tail)
      }
    }
  }

  def decodeRowsAs[A]: DecodeRowsAs[A] = new DecodeRowsAs
}

final class DecodeRowsAs[A](private val dummy: Boolean = true) extends AnyVal {

  def usingPositionOnly[R, E1 >: RowFailure](
    rows: ZStream[R, E1, Row],
  )(implicit decoder: RowDecoder.FromPositionOnly[A]): ZStream[R, E1, A] = {
    rows.mapM { row =>
      decoder.decode(row)
    }
  }

  def usingHeaderInfo[R, E1 >: RowFailure](
    rows: ZStream[R, E1, Row],
  )(implicit decoder: RowDecoder.FromHeaderInfo[A]): ZStream[R, E1, A] = {
    val readHeaderThenAllRows = CsvDecoder.readHeaderInfo(rows).map {
      case Some((header, dataRows)) =>
        dataRows.mapM { row =>
          val env = Has.allOf(header, row.rowContext)
          decoder.decode(row).provide(env)
        }
      case None =>
        ZStream.empty
    }
    // flatten the ZManaged ZStream into a single ZStream
    ZStream.managed(readHeaderThenAllRows).flatten
  }

  def providedHeader[R, E1 >: RowFailure](
    header: HeaderCtx,
    dataRows: ZStream[R, E1, Row],
  )(implicit
    decoder: RowDecoder.FromHeaderInfo[A],
  ): ZStream[R, E1, A] = {
    usingPositionOnly(dataRows)(decoder.withFixedHeader(header))
  }
}
