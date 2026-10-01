-- liquibase formatted sql

-- changeset kaloyan:1790882019121-1 splitStatements:false
CREATE TABLE transactions (id BIGINT AUTO_INCREMENT NOT NULL, amount DECIMAL(18, 2) NOT NULL, currency CHAR(3) NOT NULL, `description` VARCHAR(255) NOT NULL, year_ INT NOT NULL, CONSTRAINT transactionsPK PRIMARY KEY (id));

