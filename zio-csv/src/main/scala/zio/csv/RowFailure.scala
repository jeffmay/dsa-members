package zio.csv

import zio.{Tag, URIO, ZIO}

import scala.util.control.NoStackTrace
import scala.util.matching.Regex

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

sealed abstract class DecodingFailure(
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
) extends DecodingFailure(
    rowIndex,
    s"Expected a header column named '$expectedColumnName'",
    None,
  )

final case class InvalidColumnIndex(
  override val rowIndex: Long,
  expectedColumnIndex: Int,
) extends DecodingFailure(
    rowIndex,
    s"Expected at least ${expectedColumnIndex + 1} columns",
    None,
  )

sealed trait CellDecodingFailure extends DecodingFailure {
  def columnIndex: Int
}

object CellDecodingFailure {

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
) extends DecodingFailure(
    rowIndex,
    s"Expected cell at column ${columnIndex + 1} to be of type ${Tag[A].tag}. Caused by:\n$cause",
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
) extends DecodingFailure(
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
