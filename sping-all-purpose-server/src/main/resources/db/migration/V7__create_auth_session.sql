-- One revocable session per login. Only the hash of the current refresh token is stored.
CREATE TABLE auth_session (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_auth_session_user ON auth_session(user_id);
CREATE INDEX ix_auth_session_expiry ON auth_session(expires_at);
