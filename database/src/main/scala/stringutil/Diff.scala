package org.dsasf.members
package stringutil

import zio.Chunk

import scala.collection.mutable.ArrayBuffer

final case class Diff private (
  left: String,
  right: String,
  parts: Chunk[Diff.Part],
) {

  def isSame: Boolean =
    parts.length == 1 && parts(0).isInstanceOf[Diff.Part.Same]

  def firstDiff: Option[(Int, String, String)] =
    // NOTE: this only works if the sequence of parts is [Different,] Same, Different, Same, ...
    parts.zipWithIndex.collectFirst {
      case (
            Diff.Part.Different(leftChunk, leftStart, rightChunk, rightStart),
            idx,
          ) =>
        val prev3 = Option.unless(idx == 0) {
          val prevPart = parts(idx - 1)
          val prevText = {
            val leftText = prevPart.leftChunk.asString
            if (!leftText.isBlank) leftText
            else prevPart.rightChunk.asString
          }
          prevText.takeRight(3)
        }
        val post3 = Option.when(idx < parts.length) {
          val nextPart = parts(idx + 1)
          val postText = {
            val leftText = nextPart.leftChunk.asString
            if (!leftText.isBlank) leftText
            else nextPart.rightChunk.asString
          }
          postText.take(3)
        }
        val prefix = prev3.toList.flatMap("..." :: _ :: Nil).mkString
        val suffix = post3.toList.flatMap(_ :: "..." :: Nil).mkString
        val leftDiff = (prefix :: "[" :: leftChunk.asString :: "]" :: suffix :: Nil).mkString
        val rightDiff = (prefix :: "[" :: rightChunk.asString :: "]" :: suffix :: Nil).mkString
        (idx, leftDiff, rightDiff)
    }
}

object Diff {

  def apply(left: String, right: String): Diff = {
    val lChars = Chunk.fromArray(left.toCharArray)
    val rChars = Chunk.fromArray(right.toCharArray)
    val b = Chunk.newBuilder[Part]
    var i = 0
    var lStart = 0
    var rStart = 0
    var isSame = true
    while ((i + lStart) < lChars.length && (i + rStart) < rChars.length) {
      val lIdx = i + lStart
      val rIdx = i + rStart
      val lChar = lChars(lIdx)
      val rChar = rChars(rIdx)
      if (!isSame && rChar == lChar) {
        b += Part.Different(
          lChars.slice(lStart, lIdx),
          lStart,
          rChars.slice(rStart, i),
          rStart,
        )
        isSame = true
        lStart = lIdx
        rStart = rIdx
      } else if (isSame && rChar != lChar) {
        b += Part.Same(lChars.slice(lStart, lIdx), lStart, rStart)
        isSame = false
        lStart = lIdx
        rStart = rIdx
      }
      i += 1
    }
    if (isSame) {
      b += Part.Same(lChars.slice(lStart, lStart + i), lStart, rStart)
      lStart += i
      rStart += i
    }
    inline def isFinished = lStart == lChars.length && rStart == rChars.length
    if (!isSame || !isFinished) {
      b += Part.Different(
        lChars.drop(lStart),
        lStart,
        rChars.drop(rStart),
        rStart,
      )
    }
    Diff(left, right, b.result())
  }

  enum Part(
    val leftChunk: Chunk[Char],
    val leftStart: Int,
    val rightChunk: Chunk[Char],
    val rightStart: Int,
  ) {

    case Same(
      content: Chunk[Char],
      override val leftStart: Int,
      override val rightStart: Int,
    ) extends Part(content, leftStart, content, rightStart)

    case Different(
      override val leftChunk: Chunk[Char],
      override val leftStart: Int,
      override val rightChunk: Chunk[Char],
      override val rightStart: Int,
    ) extends Part(leftChunk, leftStart, rightChunk, rightStart)
  }
}
