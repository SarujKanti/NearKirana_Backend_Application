package com.nearkirana.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * One OTP request ("challenge"). The app receives only the id (challengeId);
 * the OTP travels by SMS. We store just a hash of the OTP.
 */
@Entity
@Table(name = "otp_challenges")
public class OtpChallenge {

    @Id
    private UUID id; // random UUID created in Java: impossible to guess

    @Column(name = "mobile_number", nullable = false, length = 15)
    private String mobileNumber;

    @Column(name = "otp_hash", nullable = false, length = 64)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "request_ip", nullable = false, length = 45)
    private String requestIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OtpChallenge() {
    }

    public OtpChallenge(UUID id, String mobileNumber, String otpHash,
                        Instant expiresAt, String requestIp, Instant createdAt) {
        this.id = id;
        this.mobileNumber = mobileNumber;
        this.otpHash = otpHash;
        this.expiresAt = expiresAt;
        this.requestIp = requestIp;
        this.createdAt = createdAt;
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public void recordFailedAttempt() {
        this.attemptCount++;
    }

    public void consume(Instant now) {
        this.consumedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public String getRequestIp() {
        return requestIp;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
