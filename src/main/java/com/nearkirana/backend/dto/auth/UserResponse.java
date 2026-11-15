package com.nearkirana.backend.dto.auth;

import com.nearkirana.backend.entity.Role;
import com.nearkirana.backend.entity.User;

/**
 * What the app is allowed to see about a user.
 * We never return the User entity directly: a DTO decides exactly which fields leave the server.
 */
public record UserResponse(
        Long id,
        String mobileNumber,
        Role role,
        String fullName,
        String preferredLanguage,
        boolean isVerified
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getMobileNumber(),
                user.getRole(),
                user.getFullName(),
                user.getPreferredLanguage(),
                user.isVerified());
    }
}
