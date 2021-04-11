package zio
package csv

import stream.{ZSink, ZStream}

object CsvDecoder {

  /** Peels off the header row(s) and returns the [[HeaderInfo]] as well as
    * another stream containing the rest of the rows of data.
    */
  def readHeaderInfo[R, E >: RowFailure](
    rows: ZStream[R, E, Row],
  ): ZIO[R, E, Option[(HeaderInfo, ZStream[R, E, Row])]] = {
    rows.peel(ZSink.head[Row]).useNow.map { case (maybeHead, tail) ⇒
      maybeHead.map { firstRow ⇒
        val header = HeaderInfo.fromRow(firstRow)
        (header, tail)
      }
    }
  }

  def decodeRowsAs[A]: CsvDecoder[Any, A] = new DecodeAllNoHeader

  def decodeRowsUsingHeaderInfoAs[A]: CsvDecoder[Has[HeaderInfo], A] =
    new DecodeAllWithHeader
}

sealed trait CsvDecoder[H, A] extends Any {
  def apply[R, E1 >: RowFailure <: Throwable](
    rows: ZStream[R, E1, Row],
  )(implicit
    decoder: RowDecoder[H, A],
  ): ZStream[R, E1, A]
}

final private class DecodeAllNoHeader[A] private[csv] (
  private val dummy: Boolean = true,
) extends AnyVal
  with CsvDecoder[Any, A] {

  override def apply[R, E1 >: RowFailure](
    rows: ZStream[R, E1, Row],
  )(implicit decoder: RowDecoder.FromPositionOnly[A]): ZStream[R, E1, A] = {
    rows.zipWithIndex.mapM { case (row, idx) ⇒
      val ctx = Has(RowCtx(idx))
      decoder.decode(row).provide(ctx)
    }
  }
}

final private class DecodeAllWithHeader[A] private[csv] (
  private val dummy: Boolean = true,
) extends AnyVal
  with CsvDecoder[Has[HeaderInfo], A] {

  override def apply[R, E1 >: RowFailure <: Throwable](
    rows: ZStream[R, E1, Row],
  )(implicit decoder: RowDecoder.FromHeaderInfo[A]): ZStream[R, E1, A] = {
    val maybeResults = CsvDecoder.readHeaderInfo(rows).orDie.map {
      case Some((header, dataRows)) ⇒
        println(s"HEADER = ${header.columns}")
        dataRows.zipWithIndex.map { case (row, idx) ⇒
          val ctx = Has.allOf(header, RowCtx(idx))
          println(s"LINE $idx: ${row.cells}")
          decoder.decode(row).provide(ctx)
        }
      case None ⇒
        ZStream.empty
    }
    ZStream.fromEffectOption(maybeResults).flatten.mapM(identity)
  }

}
