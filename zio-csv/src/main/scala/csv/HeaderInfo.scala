package zio
package csv

trait HeaderInfo[EnvMin, +Env <: EnvMin] extends Any with CsvEnv[EnvMin, Env] {

  override protected type Self[+env <: EnvMin] <: HeaderInfo[EnvMin, env]

  def headerContext[H >: Env](implicit
    ev: H <:< HeaderCtx,
    tag: Tag[H],
  ): HeaderCtx =
    ev(toEnv.get[H])

  def optionalHeaderContext: Option[HeaderCtx] = getOption[HeaderCtx]

  def addHeaderContext(headerCtx: HeaderCtx): Self[HeaderCtx & Env] =
    build(toEnv.add[HeaderCtx](headerCtx))
}

/**
  * The header row of the CSV file, carried around as a context parameter in the environment for [[RowWithColNames]]s to use.
  */
final case class HeaderCtx(headerRow: Chunk[String]) {

  lazy val columnIndexByName: Map[String, Int] =
    headerRow.zipWithIndex.toMap

  lazy val columnNameByIndex: Map[Int, String] =
    headerRow.indices.zip(headerRow).toMap
}

object HeaderCtx {

  def fromHeaderRow(headerRow: IterableOnce[String]): HeaderCtx =
    new HeaderCtx(Chunk.from(headerRow))
}
