package com.nearkirana.backend.dto.shop;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Body for creating (POST) and fully updating (PUT) a shop.
 * There is NO ownerId field: the owner always comes from the logged-in user's token.
 *
 * latitude / longitude come from the phone's GPS or a map pin. The ranges
 * below are India's rough borders, which also catches swapped values.
 */
public record ShopRequest(
        @NotBlank(message = "Shop name is required")
        @Size(max = 150, message = "Shop name must be at most 150 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @NotBlank(message = "Contact number is required")
        String contactNumber,

        @NotBlank(message = "Address is required")
        @Size(max = 255, message = "Address must be at most 255 characters")
        String addressLine,

        @NotBlank(message = "Village or city is required")
        @Size(max = 100, message = "Village or city must be at most 100 characters")
        String villageOrCity,

        @Size(max = 100, message = "District must be at most 100 characters")
        String district,

        @NotBlank(message = "State is required")
        @Size(max = 100, message = "State must be at most 100 characters")
        String state,

        @NotBlank(message = "Pincode is required")
        @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Pincode must be 6 digits and not start with 0")
        String pincode,

        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "6.0", message = "Shop location must be inside India (latitude 6 to 38)")
        @DecimalMax(value = "38.0", message = "Shop location must be inside India (latitude 6 to 38)")
        Double latitude,

        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "68.0", message = "Shop location must be inside India (longitude 68 to 98)")
        @DecimalMax(value = "98.0", message = "Shop location must be inside India (longitude 68 to 98)")
        Double longitude,

        Boolean deliveryAvailable, // optional, default false

        @DecimalMin(value = "0.5", message = "Delivery radius must be at least 0.5 km")
        @DecimalMax(value = "50.0", message = "Delivery radius must be at most 50 km")
        @Digits(integer = 3, fraction = 2, message = "Delivery radius can have at most 2 decimal places")
        BigDecimal deliveryRadiusKm
) {
}
