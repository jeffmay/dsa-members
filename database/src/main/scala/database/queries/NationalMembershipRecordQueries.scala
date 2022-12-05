package org.dsasf.members
package database.queries

import database.models.{User, UserId}
import database.models.national.*

import io.getquill.*

class NationalMembershipRecordQueries(ctx: PostgresJAsyncContext[SnakeCaseWithPluralTableNames]) {
  import ctx.*

  inline def findByUserId(inline userId: UserId) = quote {
    query[NationalMembershipRecord].filter(_.userId == lift(Some(userId)))
  }
}
