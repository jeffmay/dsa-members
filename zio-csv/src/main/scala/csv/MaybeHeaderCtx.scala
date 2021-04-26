//package zio
//package csv
//
//sealed abstract class MaybeHeaderCtx[R] {
//
//  def headerCtx(ctx: R): Option[HeaderCtx]
//
//  final def columnName(ctx: R, index: Int): Option[String] =
//    headerCtx(ctx).flatMap(_.columnNameByIndex.get(index))
//
//  final def columnIndex(ctx: R, name: String): Option[Int] =
//    headerCtx(ctx).flatMap(_.columnIndexByName.get(name))
//}
//
//sealed trait NoHeaderNameLowPriorityDefault {
//  implicit def noHeaderCtx[A]: MaybeHeaderCtx[A] =
//    new MaybeHeaderCtx[A] {
//      final override def headerCtx(ctx: A): Option[HeaderCtx] = None
//    }
//}
//
//object MaybeHeaderCtx extends NoHeaderNameLowPriorityDefault {
//  @inline def apply[R](implicit m: MaybeHeaderCtx[R]): MaybeHeaderCtx[R] = m
//  implicit def hasHeaderCtx[A <: Has[HeaderCtx]]: MaybeHeaderCtx[A] =
//    new MaybeHeaderCtx[A] {
//      final override def headerCtx(ctx: A): Option[HeaderCtx] =
//        Some(ctx.get[HeaderCtx])
//    }
//}
