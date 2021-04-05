package zio.csv

import zio.{Has, UIO}
import zio.stream.{Stream, UStream, ZSink, ZStream}

sealed trait Parse[+E, +A] extends Any {
  def fromLines(lines: UStream[String]): Stream[E, A]
}

object Parse {
  private[csv] def splitRow(line: String): Row = {
    // this is safe because the array is unused outside of this local scope
    Row.unsafeFromArray(line.split(','))
  }

  def rows: Parse[RowFailure, Row] = ParseRows

  def rowsAs[A](implicit
    decoder: RowDecoder.FromPositionOnly[A],
  ): Parse[RowFailure, A] =
    new ParseDecodeNoHeader(decoder)

  def rowsWithHeaderAs[A](implicit
    decoder: RowDecoder.FromHeaderCtx[A],
  ): Parse[RowFailure, A] =
    new ParseDecodeWithHeader(decoder)
}

// not sure if this is useful enough to expose
private[csv] object ParseRows extends Parse[Nothing, Row] {
  override def fromLines(lines: UStream[String]): Stream[Nothing, Row] =
    lines.map(Parse.splitRow)
}

sealed trait ParseWithDecoder[A] extends Any with Parse[RowFailure, A]

final private class ParseDecodeNoHeader[A] private[csv] (
  private val decoder: RowDecoder.FromPositionOnly[A],
) extends AnyVal
  with ParseWithDecoder[A] {
  override def fromLines(lines: UStream[String]): Stream[RowFailure, A] = {
    lines.zipWithIndex.mapM { case (line, idx) ⇒
      val row = Parse.splitRow(line)
      val ctx = Has(RowCtx(idx))
      decoder.decode(row).provide(ctx)
    }
  }
}

final private class ParseDecodeWithHeader[A] private[csv] (
  private val decoder: RowDecoder.FromHeaderCtx[A],
) extends AnyVal
  with ParseWithDecoder[A] {

  private def readHeader(lines: UStream[String]): UIO[Option[(
    UStream[String],
    HeaderCtx,
  )]] = {
    lines.peel(ZSink.head).useNow.map { case (maybeHead, tail) ⇒
      maybeHead.map { firstLine ⇒
        val row = Parse.splitRow(firstLine)
        val header = HeaderCtx(row.cells)
        (tail, header)
      }
    }
  }

  override def fromLines(lines: UStream[String]): Stream[RowFailure, A] = {
    val maybeResults = readHeader(lines).map {
      case Some((rows, header)) ⇒
        println(s"HEADER = ${header.columns}")
        rows.zipWithIndex.map { case (line, idx) ⇒
          val row = Parse.splitRow(line)
          val ctx = Has.allOf(header, RowCtx(idx))
          println(s"LINE $idx: $line")
          decoder.decode(row).provide(ctx)
        }
      case None ⇒
        ZStream.empty
    }
    ZStream.fromEffectOption(maybeResults).flatten.mapM(identity)
  }

}
