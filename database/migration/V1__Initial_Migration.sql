CREATE TABLE IF NOT EXISTS users (
    id                        UUID NOT NULL PRIMARY KEY,
    created                   TIMESTAMP NOT NULL DEFAULT now(),
    modified                  TIMESTAMP NOT NULL DEFAULT now(),
    full_name                 VARCHAR NOT NULL,
    pronouns                  VARCHAR,
    primary_email_address     VARCHAR,
--    auth_email                VARCHAR NOT NULL UNIQUE, -- TODO: Migrate to auth0 id
    do_not_call               BOOLEAN NOT NULL DEFAULT false,
    do_not_email              BOOLEAN NOT NULL DEFAULT false,
    notes                     VARCHAR
--    address                   VARCHAR,
--    city                      VARCHAR,
--    zipcode                   VARCHAR(10),
--    dues_paid_overriden_on    TIMESTAMP,
--    onboarded_on              TIMESTAMP,
--    suspended_on              TIMESTAMP,
--    left_chapter_on           TIMESTAMP
);

CREATE TABLE IF NOT EXISTS email_addresses (
    id              UUID NOT NULL PRIMARY KEY,
    modified        TIMESTAMP NOT NULL DEFAULT now(),
    user_id         UUID NOT NULL,
    address         VARCHAR NOT NULL,
    unified_address VARCHAR NOT NULL,
    name            VARCHAR,
    country_code    INT, -- NULL assumes local phone number (i.e. +1 if chapter is in USA)
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS phone_numbers (
    id            UUID NOT NULL PRIMARY KEY,
    modified      TIMESTAMP NOT NULL DEFAULT now(),
    user_id       UUID NOT NULL,
    number        BIGINT NOT NULL,
    name          VARCHAR,
    country_code  INT, -- NULL = USA (+1)
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS national_membership_records (
    id                UUID NOT NULL PRIMARY KEY,
    imported          TIMESTAMP NOT NULL DEFAULT now(),
    updated           TIMESTAMP NOT NULL DEFAULT now(),
    user_id           UUID,
    active            BOOLEAN NOT NULL,
    join_date         TIMESTAMP NOT NULL,
    dues_paid_until   TIMESTAMP NOT NULL,
    ak_id             INT,
    dsa_id            INT,
    do_not_call       BOOLEAN NOT NULL,
    first_name        VARCHAR NOT NULL,
    middle_name       VARCHAR,
    last_name         VARCHAR NOT NULL,
    address_line_1    VARCHAR NOT NULL,
    address_line_2    VARCHAR,
    city              VARCHAR NOT NULL,
    zipcode           VARCHAR(10) NOT NULL,
    country           VARCHAR NOT NULL DEFAULT 'United States',
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL ON UPDATE CASCADE
);
