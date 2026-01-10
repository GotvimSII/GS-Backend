CREATE TABLE users (
    id UUID PRIMARY KEY,
    email email UNIQUE NOT NULL,
    username VARCHAR(32) UNIQUE NOT NULL,
    password_hash VARCHAR(128) NOT NULL
);