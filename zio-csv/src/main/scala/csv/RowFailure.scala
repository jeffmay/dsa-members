package zio
package csv

import scala.util.control.NoStackTrace
import scala.util.matching.Regex

/** An error during parsing or decoding a CSV.
  *
  * Typically, this is returned and handled through a typed error channel, however, it can be throw as well.
  *
  * @note this extends [[Product]] so that all subclasses must define [[Equals]] properly.
  */
sealed trait ReadingFailure extends Exception with Product with NoStackTrace

/** An error while parsing a chunk of bytes into CSV rows.
  * @see [[ReadingFailure]]
  */
sealed trait ParsingFailure extends ReadingFailure

/** An exception was caught from an underlying library while parsing.
  * @see [[ReadingFailure]]
  */
final case class ParsingException(cause: Throwable)
  extends Exception(
    s"Parsing failed due to exception: $cause",
    cause,
  )
  with ParsingFailure

/** An error while parsing or decoding a row of a CSV.
  * @see [[ReadingFailure]]
  */
sealed trait RowFailure extends ReadingFailure {
  def rowIndex: Long
}

/** An error while parsing a row of a CSV.
  * @see [[ReadingFailure]]
  */
sealed abstract class RowParsingFailure(
  override val rowIndex: Long,
  val reason: String,
  val cause: Option[Throwable],
) extends Exception(s"Parsing failure at row $rowIndex: $reason", cause.orNull)
  with RowFailure
  with ParsingFailure

/** The given line of text cannot be parsed using the expected CSV format.
  * @see [[ReadingFailure]]
  */
final case class RowInvalidSyntax(
  override val rowIndex: Long,
  syntaxError: String,
  override val cause: Option[Throwable],
) extends RowParsingFailure(
    rowIndex,
    s"Invalid CSV row syntax: $syntaxError",
    cause,
  )

/** An error while decoding a row of a CSV into an expected type.
  * @see [[ReadingFailure]]
  */
sealed abstract class DecodingFailure(
  override val rowIndex: Long,
  val reason: String,
  cause: Option[Throwable],
) extends Exception(
    s"Decoding failure at row $rowIndex: $reason",
    cause.orNull,
  )
  with RowFailure

/** An error caused by attempting to reference a column by a name that does not exist in the [[HeaderCtx]].
  * @see [[ReadingFailure]]
  */
final case class InvalidColumnName(
  override val rowIndex: Long,
  expectedColumnName: String,
) extends DecodingFailure(
    rowIndex,
    s"Expected a header column named '$expectedColumnName'",
    None,
  )

/** An error caused by attempting to reference a column by an index that does not exist in the [[RowCtx]].
  * @see [[ReadingFailure]]
  */
final case class InvalidColumnIndex(
  override val rowIndex: Long,
  expectedColumnIndex: Int,
) extends DecodingFailure(
    rowIndex,
    s"Expected at least ${expectedColumnIndex + 1} columns",
    None,
  )

/** An error caused by attempting to decode the contents of a [[Cell]] into the expected result type.
  * @see [[ReadingFailure]]
  */
sealed trait CellDecodingFailure extends DecodingFailure {
  def columnIndex: Int
}

object CellDecodingFailure {

  /** Create [[CellDecodingFailure]] without a known type using the [[CellDecoder.MinCtx]] and the given message.
    */
  def fromMessage(reason: String): URIO[
    CellDecoder.MinCtx,
    CellDecodingFailure,
  ] = {
    ZIO.services[RowCtx, CellCtx].map { case (row, cell) =>
      GenericCellDecodingFailure(
        row.rowIndex,
        cell.columnIndex,
        reason,
      )
    }
  }

  /** Convert an exception into a [[CellDecodingException]] of a given type using the [[CellDecoder.MinCtx]].
    */
  def fromExceptionDecodingAs[A : Tag](cause: Throwable): URIO[
    CellDecoder.MinCtx,
    CellDecodingTypedFailure[A],
  ] = {
    ZIO.services[RowCtx, CellCtx].map { case (row, cell) =>
      CellDecodingException[A](
        row.rowIndex,
        cell.columnIndex,
        cause,
      )
    }
  }
}

/** A generic error occurred while decoding a [[Cell]] into some expected result.
  *
  * @note this can be used by other libraries for defining custom error messages
  *       that do not require any expected type information. If you have the type
  *       [[Tag]], then you should use some [[CellDecodingTypedFailure]] instead.
  *
  * @see [[ReadingFailure]]
  */
final case class GenericCellDecodingFailure(
  override val rowIndex: Long,
  columnIndex: Int,
  override val reason: String,
//  accumulated: IndexedSeq[CellDecodingFailure] = IndexedSeq(),
) extends DecodingFailure(rowIndex, reason, None)
  with CellDecodingFailure

/** A [[CellDecodingFailure]] with a known expected type.
  * @see [[ReadingFailure]]
  */
sealed trait CellDecodingTypedFailure[A] extends CellDecodingFailure {
  def expectedType: Tag[A]
  def withNewExpectedType[B : Tag]: CellDecodingTypedFailure[B]
}

/** An exception was caught while attempting to decode a [[Cell]] into an expected type.
  * @see [[ReadingFailure]]
  */
final case class CellDecodingException[A : Tag](
  override val rowIndex: Long,
  columnIndex: Int,
  cause: Throwable,
) extends DecodingFailure(
    rowIndex,
    s"Expected cell at column ${columnIndex + 1} to be of type ${Tag[A].tag}. Caused by:\n$cause",
    None, // the exception is already printed as part of this message
  )
  with CellDecodingTypedFailure[A] {

  override def initCause(cause: Throwable): Throwable = super.initCause(cause)

  override val expectedType: Tag[A] = Tag[A]
  override def withNewExpectedType[B : Tag]: CellDecodingTypedFailure[B] =
    copy[B]()
}

/** The given [[Cell]] content did not match the [[expectedPattern]].
  *
  * The [[patternType]] determines the language of the pattern (ex. "regular expression").
  *
  * @see [[ReadingFailure]]
  */
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

object CellInvalidFormat {
  final val RegexPatternType = "regular expression"
}

/** The given [[Cell]] content did not match the expected regular expression pattern.
  * @see [[ReadingFailure]]
  */
final case class CellInvalidUnmatchedRegex[A : Tag](
  override val rowIndex: Long,
  columnIndex: Int,
  expectedPattern: Regex,
) extends CellInvalidFormat[A](
    rowIndex,
    columnIndex,
    CellInvalidFormat.RegexPatternType,
    expectedPattern.pattern.pattern,
  ) {
  override def withNewExpectedType[B : Tag]: CellDecodingTypedFailure[B] =
    copy[B]()
}
