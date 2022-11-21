// package org.dsasf.members
// package database.ddl

// import doobie.Fragment
// import doobie.implicits._

// object NationalMembershipRecords extends CreateTableOp with DropTableOp {

//   override val createTable: Fragment = sql"""
//     CREATE TABLE IF NOT EXISTS national_membership_records (
//       id                INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
//       date_imported     TIMESTAMP NOT NULL DEFAULT now(),
//       date_updated      TIMESTAMP NOT NULL DEFAULT now(),
//       user_id           INT,
//       active            BOOLEAN NOT NULL,
//       join_date         TIMESTAMP NOT NULL,
//       dues_paid_until   TIMESTAMP NOT NULL,
//       ak_id             INT,
//       dsa_id            INT,
//       do_not_call       BOOLEAN NOT NULL,
//       first_name        VARCHAR NOT NULL,
//       middle_name       VARCHAR,
//       last_name         VARCHAR NOT NULL,
//       address_line_1    VARCHAR NOT NULL,
//       address_line_2    VARCHAR,
//       city              VARCHAR NOT NULL,
//       zipcode           VARCHAR(10) NOT NULL,
//       country           VARCHAR NOT NULL DEFAULT 'United States',
//       CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL ON UPDATE CASCADE
//     )
//   """

//   override val dropTable: doobie.Fragment =
//     sql"DROP TABLE national_membership_records"
// }
