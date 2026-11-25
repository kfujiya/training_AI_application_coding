-- Run this script while connected as the test user. The tables are created
-- in that user's current schema (search_path).

CREATE TABLE books (
    isbn        varchar(13) PRIMARY KEY,
    title       text NOT NULL,
    volume      integer,
    author      text NOT NULL
);

CREATE TABLE users (
    user_id     bigint PRIMARY KEY,
    full_name   text NOT NULL
);

CREATE TABLE loan_records (
    user_id     bigint NOT NULL REFERENCES users (user_id),
    isbn        varchar(13) NOT NULL REFERENCES books (isbn),
    loaned_at   timestamptz NOT NULL,
    returned_at timestamptz,
    PRIMARY KEY (user_id, isbn, loaned_at),
    CHECK (returned_at IS NULL OR returned_at >= loaned_at)
);

CREATE TABLE reservations (
    user_id     bigint NOT NULL REFERENCES users (user_id),
    isbn        varchar(13) NOT NULL REFERENCES books (isbn),
    reserved_at timestamptz NOT NULL,
    loaned_at   timestamptz,
    PRIMARY KEY (user_id, isbn, reserved_at),
    CHECK (loaned_at IS NULL OR loaned_at >= reserved_at)
);
