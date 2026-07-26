-- app_user is the identity table for authentication only. It carries credentials and a
-- role, nothing else. Domain entities (member, etc.) live in their own tables and link
-- to app_user via a foreign key. Name is "app_user" because "user" is reserved in
-- PostgreSQL.
--
-- This migration ships with the project so the tutorial does not have to cover security.

CREATE SEQUENCE app_user_seq START WITH 1000 INCREMENT BY 1;

CREATE TABLE app_user (
    id            BIGINT       PRIMARY KEY DEFAULT nextval('app_user_seq'),
    username      VARCHAR(60)  NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('MEMBER', 'LIBRARIAN')),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_app_user_role ON app_user (role);
