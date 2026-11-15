package com.nearkirana.backend.sms;

/**
 * Abstraction for sending SMS.
 *
 * AuthService depends only on this interface, never on a specific provider.
 * Today: ConsoleSmsSender (development only).
 * Later: Msg91SmsSender implements SmsSender, and no auth code changes.
 */
public interface SmsSender {

    /**
     * @param mobileNumber normalized number, e.g. +919876543210
     * @param otp          the 6-digit code to deliver
     */
    void sendOtp(String mobileNumber, String otp);
}
