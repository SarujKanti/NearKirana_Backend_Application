-- =====================================================================
-- V5: Did the user type an OTP for THIS particular OTP request?
-- (mobile_login_records.entered_otp is the per-number history; this is per attempt.)
-- =====================================================================
ALTER TABLE otp_challenges
    ADD COLUMN otp_entered BOOLEAN NOT NULL DEFAULT FALSE;

-- Old challenges with wrong attempts, or already used, clearly had an OTP entered.
UPDATE otp_challenges SET otp_entered = TRUE WHERE attempt_count > 0;
