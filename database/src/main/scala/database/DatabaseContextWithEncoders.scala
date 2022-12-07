package org.dsasf.members
package database

import database.common.DefaultNamingStrategy

import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.typesafe.config.Config
import io.getquill.util.LoadConfig
import io.getquill.{
  MappedEncoding,
  PostgresJAsyncContext,
  PostgresJAsyncContextConfig,
}

class DatabaseContextWithEncoders(pool: ConnectionPool[PostgreSQLConnection])
  extends PostgresJAsyncContext[DefaultNamingStrategy](
    DefaultNamingStrategy,
    pool,
  )
  with DatabaseEncoders {

  def this(config: PostgresJAsyncContextConfig) = this(config.pool)
  def this(config: Config) = this(PostgresJAsyncContextConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))
}
