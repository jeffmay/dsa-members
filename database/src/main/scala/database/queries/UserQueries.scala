package org.dsasf.members
package database.queries

import database.common.DefaultNamingStrategy
import models.national.NationalMembershipRecord
import models.{User, UserId}

import io.getquill.*

import java.util.UUID

class UserQueries(ctx: PostgresJAsyncContext[DefaultNamingStrategy]) {
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
