package zio.util

import scala.deriving.Mirror

trait SubTypeOrdinal[-E]:
  extension (value: E) def ordinal: Int

object SubTypeOrdinal:
  inline def derived[E](using m: Mirror.SumOf[E]): SubTypeOrdinal[E] = m.ordinal _
