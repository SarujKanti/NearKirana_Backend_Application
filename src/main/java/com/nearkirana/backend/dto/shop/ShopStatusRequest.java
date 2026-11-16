package com.nearkirana.backend.dto.shop;

import jakarta.validation.constraints.NotNull;

/** Body for PATCH .../status: open or close the shop for today. { "isOpen": false } */
public record ShopStatusRequest(
        @NotNull(message = "isOpen is required (true or false)")
        Boolean isOpen
) {
}
