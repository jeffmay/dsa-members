package zio.util

trait ExampleReader[+E] {
  def readString(value: String): E
}

object ExampleReader:

  def enumOf(
    companion: Singleton & EnumCompanion
  )(using
    DiscriminatorFormat,
    SubTypeDiscriminator[companion.EntryType],
  ): ExampleReader[companion.EntryType] =
    import companion.given_EnumEntries_EntryType
    enumOfType[companion.EntryType]

  given enumOfType[E : EnumEntries : SubTypeDiscriminator](using format: DiscriminatorFormat): ExampleReader[E] =
    new ExampleReader[E]:
      private val mapDiscriminatorToEntry: Map[String, E] =
        summon[EnumEntries[E]].entries.map { e =>
          format(e.discriminator) -> e
        }.toMap
      override def readString(value: String): E = mapDiscriminatorToEntry(value)
