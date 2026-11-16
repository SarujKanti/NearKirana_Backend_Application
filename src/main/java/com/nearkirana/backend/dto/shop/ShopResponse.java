package com.nearkirana.backend.dto.shop;

import com.nearkirana.backend.entity.Shop;

import java.math.BigDecimal;
import java.time.Instant;

/** What the shopkeeper app sees about a shop. The location is returned as plain latitude/longitude. */
public record ShopResponse(
        Long id,
        String name,
        String description,
        String contactNumber,
        String addressLine,
        String villageOrCity,
        String district,
        String state,
        String pincode,
        double latitude,
        double longitude,
        boolean isOpen,
        boolean deliveryAvailable,
        BigDecimal deliveryRadiusKm,
        Instant createdAt,
        Instant updatedAt
) {
    public static ShopResponse from(Shop shop) {
        return new ShopResponse(
                shop.getId(),
                shop.getName(),
                shop.getDescription(),
                shop.getContactNumber(),
                shop.getAddressLine(),
                shop.getVillageOrCity(),
                shop.getDistrict(),
                shop.getState(),
                shop.getPincode(),
                shop.getLatitude(),
                shop.getLongitude(),
                shop.isOpen(),
                shop.isDeliveryAvailable(),
                shop.getDeliveryRadiusKm(),
                shop.getCreatedAt(),
                shop.getUpdatedAt());
    }
}
