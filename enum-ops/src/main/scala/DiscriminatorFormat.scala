package zio.util

import scala.annotation.{implicitAmbiguous, implicitNotFound}

@implicitNotFound("No implicit or given DiscriminatorFormat. Please use one of the following imports:\n\n" +
  "  import zio.util.DiscriminatorFormat.exactCase\n" +
  "  import zio.util.DiscriminatorFormat.lowerCase\n"
)
@implicitAmbiguous("No implicit or given DiscriminatorFormat. Please use one of the following imports:\n\n" +
  "  import zio.util.DiscriminatorFormat.exactCase\n" +
  "  import zio.util.DiscriminatorFormat.lowerCase\n"
)
trait DiscriminatorFormat extends (String => String):
  override def apply(discriminator: String): String

object DiscriminatorFormat:

  given exactCase: DiscriminatorFormat = d => d

  given lowerCase: DiscriminatorFormat = d => d.toLowerCase
