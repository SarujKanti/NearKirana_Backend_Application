package com.nearkirana.backend.security;

/**
 * Creates the OTP for a mobile number.
 *
 * Production: SecureOtpGenerator (random).
 * Dev profile only: DevFixedOtpGenerator (fixed OTP for demo numbers).
 */
public interface OtpGenerator {

    /** @param mobileNumber normalized number, e.g. +919876543210 */
    String generate(String mobileNumber);
}
