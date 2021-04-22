package zio
package csv

import stream.{ZSink, ZStream}

object CsvDecoder {

  /** Peels off the header row(s) and returns the [[HeaderCtx]] as well as
    * another stream containing the rest of the rows of data.
    */
  def readHeaderInfo[R, E >: RowFailure](
    rows: ZStream[R, E, Row],
  ): ZIO[R, E, Option[(HeaderCtx, ZStream[R, E, Row])]] = {
    rows.peel(ZSink.head[Row]).useNow.map { case (maybeHead, tail) ⇒
      maybeHead.map { firstRow ⇒
        val header = HeaderCtx.fromRow(firstRow)
        (header, tail)
      }
    }
  }

  def decodeRowsAs[A]: DecodeRowsAs[A] = new DecodeRowsAs
}

final class DecodeRowsAs[A] private[csv] (
  private val dummy: Boolean = true,
) extends AnyVal {

  def usingPositionOnly[R, E >: RowFailure](
    rows: ZStream[R, E, Row],
  )(implicit decoder: RowDecoder.FromPositionOnly[A]): ZStream[R, E, A] = {
    rows.mapM { row ⇒
      decoder.decode(row)
    }
  }

  def usingHeaderInfo[R, E >: RowFailure <: Throwable](
    rows: ZStream[R, E, Row],
  )(implicit decoder: RowDecoder.FromHeaderInfo[A]): ZStream[R, E, A] = {
    val maybeResults = CsvDecoder.readHeaderInfo(rows).orDie.map {
      case Some((header, dataRows)) ⇒
        println(s"HEADER = ${header.columns}")
        dataRows.map { row ⇒
          val env = Has.allOf(header, row.rowContext)
          println(s"LINE ${row.rowIndex}: ${row.cells}")
          decoder.decode(row).provide(env).map { record ⇒
            println(s"Parsed Record: $record")
            record
          }
        }
      case None ⇒
        ZStream.empty
    }
    val z = ZStream.fromEffect(maybeResults)
    val a = z.flatten
    a.mapM(identity)
  }

  def providedHeader[R, E >: RowFailure](
    header: HeaderCtx,
    dataRows: ZStream[R, E, Row],
  )(implicit
    decoder: RowDecoder.FromHeaderInfo[A],
  ): ZStream[R, E, A] = {
    usingPositionOnly(dataRows)(decoder.withFixedHeader(header))
  }
}
