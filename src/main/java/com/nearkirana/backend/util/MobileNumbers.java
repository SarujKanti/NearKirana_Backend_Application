package com.nearkirana.backend.util;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Indian mobile number helpers.
 *
 * Users type numbers in many ways: "98765 43210", "+91 98765-43210",
 * "09876543210". We accept all of them and always store ONE format:
 * "+919876543210" (called E.164). One format = no duplicate accounts.
 */
public final class MobileNumbers {

    // Optional +91 / 91 / 0 prefix, then 10 digits starting with 6, 7, 8 or 9
    private static final Pattern INDIAN_MOBILE = Pattern.compile("^(?:\\+91|91|0)?([6-9]\\d{9})$");

    private MobileNumbers() {
        // utility class: no objects
    }

    /** Returns "+91XXXXXXXXXX", or empty if the input is not a valid Indian mobile number. */
    public static Optional<String> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String compact = raw.replaceAll("[\\s\\-()]", "");
        var matcher = INDIAN_MOBILE.matcher(compact);
        return matcher.matches() ? Optional.of("+91" + matcher.group(1)) : Optional.empty();
    }

    /** For logs: "+91******3210". Never log full phone numbers. */
    public static String mask(String normalized) {
        if (normalized == null || normalized.length() < 4) {
            return "****";
        }
        return "+91******" + normalized.substring(normalized.length() - 4);
    }
}
