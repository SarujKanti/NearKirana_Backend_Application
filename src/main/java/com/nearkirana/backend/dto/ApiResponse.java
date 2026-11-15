package com.nearkirana.backend.dto;

/**
 * Standard envelope for every NearKirana API response:
 * { "success": true/false, "message": "...", "data": { ... } }
 *
 * The Android app can always check "success" first, then read "data".
 */
public record ApiResponse<T>(boolean success, String message, T data) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
