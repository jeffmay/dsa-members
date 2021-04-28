package enumeratum.ops

/** A helpful wrapper around an Either from the [[EnumCodec]] for when you want to
  * handle unknown enum value names without losing the ability to throw the
  * original error.
  */
final case class UnknownEntryOr[E](
  toEither: Either[NoSuchMember, E],
) extends AnyVal {

  def toUnknownEither: Either[String, E] = toEither.left.map(_.notFoundValue)

  def toUnknownOption: Option[String] =
    toEither.left.toOption.map(_.notFoundValue)

  def toOption: Option[E] = toEither.toOption

  // TODO: Figure out if the stack trace is helpful here. Should I use scalactic / shapeless Position?
  @throws[NoSuchMember]("unrecognized enum value name")
  def toKnownOrThrow: E = toEither.fold(throw _, identity)
}

object UnknownEntryOr {

  def apply[E : EnumCodec]: Builder[E] = {
    val enum = EnumCodec[E]
    new Builder[E](str => NoSuchMember(enum.enumName, enum.valueNames, str))
  }

  final class Builder[E](private val buildException: String => NoSuchMember)
    extends AnyVal {

    def fromKnown(known: E): UnknownEntryOr[E] = fromEither(Right(known))

    def fromUnknown(unknown: String): UnknownEntryOr[E] =
      fromEither(Left(unknown))

    def fromEither(originalOrKnown: Either[String, E]): UnknownEntryOr[E] =
      UnknownEntryOr[E] {
        originalOrKnown.left.map { str =>
          buildException(str)
        }
      }
  }
}
