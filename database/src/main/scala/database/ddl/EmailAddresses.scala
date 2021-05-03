package org.dsasf.members
package database.ddl

import doobie.Fragment
import doobie.implicits._

object EmailAddresses extends CreateTableOp with DropTableOp {

  override val createTable: Fragment = sql"""
    CREATE TABLE IF NOT EXISTS email_addresses (
      id            INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
      date_created  TIMESTAMP NOT NULL DEFAULT now(),
      user_id       INT NOT NULL,
      address       VARCHAR NOT NULL,
      normalized    VARCHAR NOT NULL,
      name          VARCHAR,
      country_code  INT, -- NULL assumes local phone number (i.e. +1 if chapter is in USA)
      CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE CASCADE
    )
  """

  override val dropTable: Fragment = sql"DROP TABLE email_addresses"
}
