package com.nearkirana.backend.dto.shop;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Body for PATCH /api/v1/shopkeeper/shops/{shopId}: change ONLY the fields you send.
 *
 *   field left out (null)  -> stays as it is
 *   field sent             -> checked and changed
 *   "" for description / district -> cleared
 *
 * Every field is optional here, so there is no @NotBlank / @NotNull.
 * The other checks (@Size, @Pattern, ...) only run when a value IS sent.
 * NOT_BLANK = "if you send it, it must contain some text".
 */
public record ShopUpdateRequest(
        @Pattern(regexp = NOT_BLANK, message = "Shop name cannot be empty")
        @Size(max = 150, message = "Shop name must be at most 150 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        String contactNumber,

        @Pattern(regexp = NOT_BLANK, message = "Address cannot be empty")
        @Size(max = 255, message = "Address must be at most 255 characters")
        String addressLine,

        @Pattern(regexp = NOT_BLANK, message = "Village or city cannot be empty")
        @Size(max = 100, message = "Village or city must be at most 100 characters")
        String villageOrCity,

        @Size(max = 100, message = "District must be at most 100 characters")
        String district,

        @Pattern(regexp = NOT_BLANK, message = "State cannot be empty")
        @Size(max = 100, message = "State must be at most 100 characters")
        String state,

        @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Pincode must be 6 digits and not start with 0")
        String pincode,

        @DecimalMin(value = "6.0", message = "Shop location must be inside India (latitude 6 to 38)")
        @DecimalMax(value = "38.0", message = "Shop location must be inside India (latitude 6 to 38)")
        Double latitude,

        @DecimalMin(value = "68.0", message = "Shop location must be inside India (longitude 68 to 98)")
        @DecimalMax(value = "98.0", message = "Shop location must be inside India (longitude 68 to 98)")
        Double longitude,

        Boolean isOpen,

        Boolean deliveryAvailable,

        @DecimalMin(value = "0.5", message = "Delivery radius must be at least 0.5 km")
        @DecimalMax(value = "50.0", message = "Delivery radius must be at most 50 km")
        @Digits(integer = 3, fraction = 2, message = "Delivery radius can have at most 2 decimal places")
        BigDecimal deliveryRadiusKm
) {
    /** (?s) = also match across new lines; the text must contain one non-space character. */
    public static final String NOT_BLANK = "(?s).*\\S.*";

    /** true when the body is {} (nothing to change). */
    public boolean isEmpty() {
        return name == null && description == null && contactNumber == null && addressLine == null
                && villageOrCity == null && district == null && state == null && pincode == null
                && latitude == null && longitude == null && isOpen == null
                && deliveryAvailable == null && deliveryRadiusKm == null;
    }
}
