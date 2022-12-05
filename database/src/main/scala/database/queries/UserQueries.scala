package org.dsasf.members
package database.queries

import database.models.national.NationalMembershipRecord
import database.models.{User, UserId}

import io.getquill.*

import java.util.UUID

class UserQueries(ctx: PostgresJAsyncContext[SnakeCaseWithPluralTableNames]) {
  import ctx.*

  inline def create(inline user: User) = quote {
    query[User].insertValue(user)
  }

  inline def findAll = quote {
    query[User]
  }

  inline def findMembershipRecords(inline userId: UserId) =
    NationalMembershipRecordQueries(ctx).findByUserId(userId)
}
