-- liquibase formatted sql

-- changeset kaloyan:1790882019121-1 splitStatements:false
CREATE TABLE transactions (id BIGINT AUTO_INCREMENT NOT NULL, amount DECIMAL(18, 2) NOT NULL, currency CHAR(3) NOT NULL, `description` VARCHAR(255) NOT NULL, year_ INT NOT NULL, CONSTRAINT transactionsPK PRIMARY KEY (id));

-- changeset kaloyan:1791063470290-1 splitStatements:false
CREATE INDEX idx_transactions_year_currency ON transactions(year_, currency);

-- changeset kaloyan:1791224952245-1 splitStatements:false
CREATE TABLE exchange_rates (id BIGINT AUTO_INCREMENT NOT NULL, source_currency CHAR(3) NOT NULL, source_to_target_factor DECIMAL(18, 8) NOT NULL, target_currency CHAR(3) NOT NULL, year_ INT NOT NULL, CONSTRAINT exchange_ratesPK PRIMARY KEY (id));

-- changeset kaloyan:1791224952245-2 splitStatements:false
ALTER TABLE exchange_rates ADD CONSTRAINT uq_exchange_rates_year_currencies UNIQUE (year_, source_currency, target_currency);

/*
PLEASE NOTE

`liquibase:diff` compares two schema snapshots:
one built from your entities (by the `liquibase-hibernate7` extension)
and one from the real MySQL database.

It can only output differences in the object types that its snapshot model includes. 
    
    In open-source Liquibase,
    those types are tables, columns, primary keys, foreign keys, indexes, unique constraints, sequences and views.
    Check constraints aren't one of them.
    
    Snapshotting and diffing check constraints
    (and the `addCheckConstraint` change type)
    is a Liquibase Pro feature.

So when the extension reads your `@Table(check = {...})` annotations,
there's no `CheckConstraint` object to put them in, and they're dropped.

A missing constraint never shows up as a difference,
so no changeset is written for it.



FIX

Add them by hand as follows
(or by putting them back inline in the `CREATE TABLE` of `git checkout HEAD~1`).
Either way, a later execution of `liquibase:diff` won't try to remove them,
because it can't see them on either side.
*/
-- changeset kaloyan:1791224952245-3 splitStatements:false
ALTER TABLE exchange_rates ADD CONSTRAINT chk_exchange_rates_factor_positive CHECK (source_to_target_factor > 0);

-- changeset kaloyan:1791224952245-4 splitStatements:false
ALTER TABLE exchange_rates ADD CONSTRAINT chk_exchange_rates_distinct_currencies CHECK (source_currency != target_currency);

-- changeset kaloyan:1792698341395-1 splitStatements:false
CREATE TABLE users (id BIGINT AUTO_INCREMENT NOT NULL, email VARCHAR(128) NOT NULL, password_hash VARCHAR(128) NOT NULL, username VARCHAR(32) NOT NULL, CONSTRAINT usersPK PRIMARY KEY (id));

-- changeset kaloyan:1792698341395-2 splitStatements:false
ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);

-- changeset kaloyan:1792698341395-3 splitStatements:false
ALTER TABLE users ADD CONSTRAINT uq_users_username UNIQUE (username);

