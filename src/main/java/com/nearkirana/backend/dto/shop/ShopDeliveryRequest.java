package com.nearkirana.backend.dto.shop;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Body for PATCH .../delivery: turn home delivery on or off.
 *
 *   { "deliveryAvailable": true, "deliveryRadiusKm": 3 }  -> delivery ON, up to 3 km
 *   { "deliveryAvailable": false }                         -> delivery OFF (radius removed)
 *
 * The key is "deliveryAvailable", the same name the shop response uses.
 */
public record ShopDeliveryRequest(
        @NotNull(message = "deliveryAvailable is required (true or false)")
        Boolean deliveryAvailable,

        @DecimalMin(value = "0.5", message = "Delivery radius must be at least 0.5 km")
        @DecimalMax(value = "50.0", message = "Delivery radius must be at most 50 km")
        @Digits(integer = 3, fraction = 2, message = "Delivery radius can have at most 2 decimal places")
        BigDecimal deliveryRadiusKm
) {
}
