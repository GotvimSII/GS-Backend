CREATE TABLE sessions (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,
    refresh_token_hash BYTEA NOT NULL, -- postgre ftw

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,

    ip_address INET,
    user_agent TEXT,

    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT session_unique_hash UNIQUE (refresh_token_hash)
);