package com.nearkirana.backend.repository;

import com.nearkirana.backend.entity.OtpChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

    /** Most recent OTP for this number (used for the resend cooldown). */
    Optional<OtpChallenge> findFirstByMobileNumberOrderByCreatedAtDesc(String mobileNumber);

    long countByMobileNumberAndCreatedAtAfter(String mobileNumber, Instant since);

    long countByRequestIpAndCreatedAtAfter(String requestIp, Instant since);

    /**
     * Loads the challenge AND locks its row (SELECT ... FOR UPDATE) until the
     * transaction ends. Two simultaneous verify requests therefore cannot
     * both use the same OTP.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OtpChallenge c where c.id = :id")
    Optional<OtpChallenge> findByIdForUpdate(@Param("id") UUID id);

    /** When a new OTP is sent, older unused OTPs for that number stop working. */
    @Modifying
    @Query("update OtpChallenge c set c.consumedAt = :now "
            + "where c.mobileNumber = :mobileNumber and c.consumedAt is null")
    int invalidateActiveChallenges(@Param("mobileNumber") String mobileNumber, @Param("now") Instant now);
}
