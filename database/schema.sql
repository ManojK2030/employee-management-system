-- Employee Management System - MySQL 8 schema
-- Run once:   mysql -u root -p < database/schema.sql
-- (Optional: the backend can also create this table by itself - see README.)

CREATE DATABASE IF NOT EXISTS ems_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ems_db;

CREATE TABLE IF NOT EXISTS employees (
    id              BIGINT          NOT NULL AUTO_INCREMENT,      -- Employee ID
    full_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(150)    NOT NULL,
    phone_number    VARCHAR(20)     NOT NULL,
    department      VARCHAR(100)    NOT NULL,
    designation     VARCHAR(100)    NOT NULL,
    date_of_joining DATE            NOT NULL,
    salary          DECIMAL(12, 2)  NOT NULL,
    status          ENUM ('ACTIVE', 'INACTIVE') NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_employees_email (email),
    KEY idx_employees_department (department),
    KEY idx_employees_status (status),
    KEY idx_employees_full_name (full_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
