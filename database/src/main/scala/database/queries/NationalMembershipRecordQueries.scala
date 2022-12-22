package org.dsasf.members
package database.queries

import database.common.DefaultNamingStrategy
import models.national.*
import models.{User, UserId}

import io.getquill.*

class NationalMembershipRecordQueries(
  ctx: PostgresJAsyncContext[DefaultNamingStrategy],
) {
  import ctx.*

  inline def batchInsert(inline records: Seq[NationalMembershipRecord]) = quote {
    liftQuery(records).foreach { record =>
      query[NationalMembershipRecord].insertValue(record)
    }
  }
  
  inline def findByUserId(inline userId: UserId) = quote {
    query[NationalMembershipRecord].filter(_.userId == lift(Some(userId)))
  }
}
