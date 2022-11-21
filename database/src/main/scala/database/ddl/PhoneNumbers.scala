// package org.dsasf.members
// package database.ddl

// import doobie.implicits._
// import doobie.util.fragment.Fragment

// object PhoneNumbers extends CreateTableOp with DropTableOp {

//   override val createTable: Fragment = sql"""
//     CREATE TABLE IF NOT EXISTS phone_numbers (
//       id            INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
//       date_created  TIMESTAMP NOT NULL DEFAULT now(),
//       user_id       INT NOT NULL,
//       number        BIGINT NOT NULL,
//       name          VARCHAR,
//       country_code  INT, -- NULL = USA (+1)
//       CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE CASCADE
//     )
//   """

//   override val dropTable: doobie.Fragment = sql"DROP TABLE phone_numbers"
// }
