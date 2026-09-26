-- liquibase formatted sql

-- changeset kaloyan:1790190381140-1 splitStatements:false
CREATE TABLE transactions (id BIGINT AUTO_INCREMENT NOT NULL, amount DECIMAL(18, 2) NOT NULL, year_ INT NULL, CONSTRAINT transactionsPK PRIMARY KEY (id));

