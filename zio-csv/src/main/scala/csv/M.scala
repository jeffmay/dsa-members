package zio
package csv

sealed abstract class MaybeHeaderCtx[-R] {

  def headerCtx(ctx: R): Option[HeaderCtx]

  final def columnName(ctx: R, index: Int): Option[String] =
    headerCtx(ctx).flatMap(_.columnNameByIndex.get(index))

  final def columnIndex(ctx: R, name: String): Option[Int] =
    headerCtx(ctx).flatMap(_.columnIndexByName.get(name))
}

sealed trait NoHeaderNameLowPriorityDefault {
  implicit val noHeaderCtx: MaybeHeaderCtx[Any] =
    new MaybeHeaderCtx[Any] {
      final override def headerCtx(ctx: Any): Option[HeaderCtx] = None
    }
}

object MaybeHeaderCtx extends NoHeaderNameLowPriorityDefault {
  @inline def apply[R](implicit m: MaybeHeaderCtx[R]): MaybeHeaderCtx[R] = m
  implicit val hasHeaderCtx: MaybeHeaderCtx[Has[HeaderCtx]] =
    new MaybeHeaderCtx[Has[HeaderCtx]] {
      final override def headerCtx(ctx: Has[HeaderCtx]): Option[HeaderCtx] =
        Some(ctx.get[HeaderCtx])
    }
}
