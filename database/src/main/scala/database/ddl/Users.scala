// package org.dsasf.members
// package database.ddl

// import doobie._
// import doobie.implicits._

// object Users extends CreateTableOp with DropTableOp {

//   override val createTable: Fragment = sql"""
//     CREATE TABLE IF NOT EXISTS users (
//       id                        INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
//       date_created              TIMESTAMP NOT NULL DEFAULT now(),
//       fullName                  VARCHAR NOT NULL,
//       pronouns                  VARCHAR,
//       auth_email                VARCHAR NOT NULL UNIQUE, -- TODO: Migrate to auth0 id
//       do_not_call               BOOLEAN NOT NULL DEFAULT false,
//       do_not_email              BOOLEAN NOT NULL DEFAULT false,
//       notes                     VARCHAR,
//       address                   VARCHAR,
//       city                      VARCHAR,
//       zipcode                   VARCHAR(10),
//       dues_paid_overriden_on    TIMESTAMP,
//       onboarded_on              TIMESTAMP,
//       suspended_on              TIMESTAMP,
//       left_chapter_on           TIMESTAMP
//     )
//   """

//   override val dropTable: Fragment = sql"DROP TABLE users"
// }
