package zio
package csv

import scala.util.control.NonFatal

/**
  * This trait defines a pattern for wrapping a [[ZEnvironment]] with custom methods.
  * 
  * Under the hood, this pattern allows defining value classes that wrap a [[ZEnvironment]] and then
  * return other wrapped [[ZEnvironment]]s or desired results.
  * 
  * This is nice because it leverages [[ZEnvironment]] for the difficult task of creating a type map, while
  * requiring no additional allocations or runtime overhead for invoking the underlying methods.
  */
private[csv] trait CsvEnv[EnvMin, +Env <: EnvMin] extends Any { self =>

  /** The type of this wrapper class, so that the most specific version of this class can be returned by mixin traits. */
  protected type Self[+env <: EnvMin] <: CsvEnv[EnvMin, env]

  /** The underly environment. Used to provide to ZIO in places where the environment is required. */
  def toEnv: ZEnvironment[Env]

  /** Build an instance of this env wrapper with the most specific type possible. */
  protected def build[R <: EnvMin](env: ZEnvironment[R]): Self[R]

  protected def getOption[T : Tag]: Option[T] = CsvEnv.getOption[T](toEnv)
}

private[csv] object CsvEnv {

  /** Get a value out of the [[ZEnvironment]] (if it exists at runtime), otherwise None. */
  def getOption[T : Tag](env: ZEnvironment[Any]): Option[T] =
    Unsafe.unsafe { implicit unsafe =>
      try Some(env.unsafe.get(Tag[HeaderCtx].tag))
      catch {
        case NonFatal(_) => None
      }
    }
}
