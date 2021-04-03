package org.dsasf.members
package api

import zhttp.http._

object ==> {
  def unapply(request: Request): Option[Route] =
    Some(request.endpoint._1 → request.endpoint._2.path)
}
