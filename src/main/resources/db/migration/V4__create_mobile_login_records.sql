-- =====================================================================
-- V4: One record per mobile number that ever started a login
--
-- Created the FIRST time a number is entered (send-otp), even if the
-- person then closes the app without entering the OTP. This lets us see
-- abandoned logins, which users never become rows in "users".
-- =====================================================================
CREATE TABLE mobile_login_records (
    mobile_number         VARCHAR(15)  PRIMARY KEY,           -- +91XXXXXXXXXX
    first_requested_at    TIMESTAMPTZ  NOT NULL,              -- first time the number was entered
    last_requested_at     TIMESTAMPTZ  NOT NULL,
    otp_request_count     INTEGER      NOT NULL DEFAULT 1,
    entered_otp           BOOLEAN      NOT NULL DEFAULT FALSE, -- ever submitted an OTP (right or wrong)
    first_entered_otp_at  TIMESTAMPTZ,
    cancel_count          INTEGER      NOT NULL DEFAULT 0,     -- times the OTP screen was cancelled
    last_cancelled_at     TIMESTAMPTZ,

    CONSTRAINT ck_mobile_login_records_counts CHECK (otp_request_count >= 0 AND cancel_count >= 0)
);

-- Existing users have obviously entered an OTP already.
INSERT INTO mobile_login_records
    (mobile_number, first_requested_at, last_requested_at, otp_request_count, entered_otp, first_entered_otp_at)
SELECT mobile_number, created_at, created_at, 1, TRUE, created_at
FROM users;
