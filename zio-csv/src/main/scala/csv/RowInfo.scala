package zio
package csv

/**
  * A mixin trait for defining an interface over any [[CsvEnv]] that has a [[RowCtx]].
  * 
  * @see [[CsvEnv]] for more details on this pattern
  */
trait RowInfo[EnvMin <: RowCtx, +Env <: EnvMin] extends Any with CsvEnv[EnvMin, Env] { self =>
  
  override protected type Self[+env <: EnvMin] <: RowInfo[EnvMin, env]

  def rowIndex: Long = toEnv.get[RowCtx].rowIndex
}
