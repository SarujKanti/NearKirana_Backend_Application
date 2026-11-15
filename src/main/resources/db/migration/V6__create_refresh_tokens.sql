-- =====================================================================
-- V6: Refresh tokens (long-lived, used to get new access tokens)
--
-- The raw refresh token is NEVER stored, only its SHA-256 hash.
-- Every refresh "rotates" the token: the old one is revoked and points
-- to its replacement. All tokens from one login share a family_id, so if
-- an old (already used) token is ever presented again, we know it was
-- stolen and revoke the whole family (that device's session).
-- =====================================================================
CREATE TABLE refresh_tokens (
    id           UUID         PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    family_id    UUID         NOT NULL,          -- same for every token of one login session
    token_hash   VARCHAR(64)  NOT NULL,          -- hex SHA-256 of the raw token
    expires_at   TIMESTAMPTZ  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    revoked_at   TIMESTAMPTZ,                    -- set on rotation, logout or theft detection
    replaced_by  UUID,                           -- id of the token that replaced this one

    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id   ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);
