-- =====================================================================
-- V3: OTP verification challenges
--
-- One row = one "send OTP" request. The OTP itself is NEVER stored;
-- only an HMAC-SHA256 hash of it.
-- =====================================================================
CREATE TABLE otp_challenges (
    id             UUID         PRIMARY KEY,          -- the "challengeId" given to the app
    mobile_number  VARCHAR(15)  NOT NULL,             -- +91XXXXXXXXXX
    otp_hash       VARCHAR(64)  NOT NULL,             -- hex HMAC-SHA256, never the plain OTP
    expires_at     TIMESTAMPTZ  NOT NULL,
    attempt_count  INTEGER      NOT NULL DEFAULT 0,   -- wrong guesses so far
    consumed_at    TIMESTAMPTZ,                       -- set when used OR replaced by a newer OTP
    request_ip     VARCHAR(45)  NOT NULL,             -- IPv4 or IPv6, for rate limiting
    created_at     TIMESTAMPTZ  NOT NULL,

    CONSTRAINT ck_otp_challenges_attempts CHECK (attempt_count >= 0)
);

-- Rate limiting: "how many OTPs did this number / this IP request in the last hour?"
CREATE INDEX idx_otp_challenges_mobile_created ON otp_challenges (mobile_number, created_at DESC);
CREATE INDEX idx_otp_challenges_ip_created     ON otp_challenges (request_ip, created_at DESC);
