package com.nearkirana.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One row per mobile number that ever started a login (see V4 migration).
 * Written with atomic SQL updates in MobileLoginRecordRepository, so this
 * class is only used for reading.
 */
@Entity
@Table(name = "mobile_login_records")
public class MobileLoginRecord {

    @Id
    @Column(name = "mobile_number", length = 15)
    private String mobileNumber;

    @Column(name = "first_requested_at", nullable = false)
    private Instant firstRequestedAt;

    @Column(name = "last_requested_at", nullable = false)
    private Instant lastRequestedAt;

    @Column(name = "otp_request_count", nullable = false)
    private int otpRequestCount;

    @Column(name = "entered_otp", nullable = false)
    private boolean enteredOtp;

    @Column(name = "first_entered_otp_at")
    private Instant firstEnteredOtpAt;

    @Column(name = "cancel_count", nullable = false)
    private int cancelCount;

    @Column(name = "last_cancelled_at")
    private Instant lastCancelledAt;

    protected MobileLoginRecord() {
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public Instant getFirstRequestedAt() {
        return firstRequestedAt;
    }

    public Instant getLastRequestedAt() {
        return lastRequestedAt;
    }

    public int getOtpRequestCount() {
        return otpRequestCount;
    }

    public boolean isEnteredOtp() {
        return enteredOtp;
    }

    public Instant getFirstEnteredOtpAt() {
        return firstEnteredOtpAt;
    }

    public int getCancelCount() {
        return cancelCount;
    }

    public Instant getLastCancelledAt() {
        return lastCancelledAt;
    }
}
