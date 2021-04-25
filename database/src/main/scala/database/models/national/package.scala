package org.dsasf.members
package database.models

import io.estatico.newtype.macros.newtype

package object national {

  @newtype case class AkID(asString: String)
}
