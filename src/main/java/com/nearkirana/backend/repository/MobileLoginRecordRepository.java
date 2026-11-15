package com.nearkirana.backend.repository;

import com.nearkirana.backend.entity.MobileLoginRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

/**
 * Each write is ONE atomic SQL statement. Two requests for the same number at
 * the same moment therefore cannot create duplicate rows or lose a count.
 */
public interface MobileLoginRecordRepository extends JpaRepository<MobileLoginRecord, String> {

    /**
     * "Upsert": first time -> INSERT a new row; every later time -> UPDATE it.
     * ON CONFLICT is PostgreSQL syntax, so this is a native SQL query.
     */
    @Modifying
    @Query(value = """
            INSERT INTO mobile_login_records (mobile_number, first_requested_at, last_requested_at, otp_request_count)
            VALUES (:mobileNumber, :now, :now, 1)
            ON CONFLICT (mobile_number) DO UPDATE
               SET last_requested_at = EXCLUDED.last_requested_at,
                   otp_request_count = mobile_login_records.otp_request_count + 1
            """, nativeQuery = true)
    int recordOtpRequested(@Param("mobileNumber") String mobileNumber, @Param("now") Instant now);

    @Modifying
    @Query("update MobileLoginRecord r set r.enteredOtp = true, "
            + "r.firstEnteredOtpAt = coalesce(r.firstEnteredOtpAt, :now) "
            + "where r.mobileNumber = :mobileNumber")
    int recordOtpEntered(@Param("mobileNumber") String mobileNumber, @Param("now") Instant now);

    @Modifying
    @Query("update MobileLoginRecord r set r.cancelCount = r.cancelCount + 1, r.lastCancelledAt = :now "
            + "where r.mobileNumber = :mobileNumber")
    int recordCancelled(@Param("mobileNumber") String mobileNumber, @Param("now") Instant now);
}
