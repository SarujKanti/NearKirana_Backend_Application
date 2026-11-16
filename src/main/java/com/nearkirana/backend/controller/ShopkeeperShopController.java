package com.nearkirana.backend.controller;

import com.nearkirana.backend.dto.ApiResponse;
import com.nearkirana.backend.dto.shop.ShopDeliveryRequest;
import com.nearkirana.backend.dto.shop.ShopRequest;
import com.nearkirana.backend.dto.shop.ShopResponse;
import com.nearkirana.backend.dto.shop.ShopStatusRequest;
import com.nearkirana.backend.dto.shop.ShopUpdateRequest;
import com.nearkirana.backend.service.ShopService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Shop profile endpoints for the SHOPKEEPER app.
 *
 * Everything under /api/v1/shopkeeper/** needs a Bearer token with role
 * SHOPKEEPER (see SecurityConfig). A CUSTOMER token gets 403 Forbidden.
 */
@RestController
@RequestMapping("/api/v1/shopkeeper/shops")
public class ShopkeeperShopController {

    private final ShopService shopService;

    public ShopkeeperShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    // POST /api/v1/shopkeeper/shops                  -> 201 Created
    @PostMapping
    public ResponseEntity<ApiResponse<ShopResponse>> createShop(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ShopRequest request) {
        ShopResponse shop = shopService.createShop(userId(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Shop created successfully", shop));
    }

    // GET /api/v1/shopkeeper/shops                   -> my shops
    @GetMapping
    public ResponseEntity<ApiResponse<List<ShopResponse>>> getMyShops(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.ok("Shops fetched successfully", shopService.getMyShops(userId(jwt))));
    }

    // GET /api/v1/shopkeeper/shops/{shopId}
    @GetMapping("/{shopId}")
    public ResponseEntity<ApiResponse<ShopResponse>> getMyShop(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long shopId) {
        return ResponseEntity.ok(ApiResponse.ok("Shop fetched successfully", shopService.getMyShop(userId(jwt), shopId)));
    }

    // PUT /api/v1/shopkeeper/shops/{shopId}          -> replace all editable details
    @PutMapping("/{shopId}")
    public ResponseEntity<ApiResponse<ShopResponse>> updateShop(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long shopId,
            @Valid @RequestBody ShopRequest request) {
        ShopResponse shop = shopService.updateShop(userId(jwt), shopId, request);
        return ResponseEntity.ok(ApiResponse.ok("Shop updated successfully", shop));
    }

    // PATCH /api/v1/shopkeeper/shops/{shopId}        -> change ONLY the fields that are sent
    @PatchMapping("/{shopId}")
    public ResponseEntity<ApiResponse<ShopResponse>> patchShop(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long shopId,
            @Valid @RequestBody ShopUpdateRequest request) {
        ShopResponse shop = shopService.patchShop(userId(jwt), shopId, request);
        return ResponseEntity.ok(ApiResponse.ok("Shop updated successfully", shop));
    }

    // PATCH /api/v1/shopkeeper/shops/{shopId}/status -> open / close the shop
    @PatchMapping("/{shopId}/status")
    public ResponseEntity<ApiResponse<ShopResponse>> updateStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long shopId,
            @Valid @RequestBody ShopStatusRequest request) {
        ShopResponse shop = shopService.updateOpenStatus(userId(jwt), shopId, request.isOpen());
        String message = request.isOpen() ? "Shop is now open" : "Shop is now closed";
        return ResponseEntity.ok(ApiResponse.ok(message, shop));
    }

    // PATCH /api/v1/shopkeeper/shops/{shopId}/delivery -> turn home delivery on / off
    @PatchMapping("/{shopId}/delivery")
    public ResponseEntity<ApiResponse<ShopResponse>> updateDelivery(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long shopId,
            @Valid @RequestBody ShopDeliveryRequest request) {
        ShopResponse shop = shopService.updateDelivery(
                userId(jwt), shopId, request.deliveryAvailable(), request.deliveryRadiusKm());
        String message = request.deliveryAvailable() ? "Delivery is now available" : "Delivery is now turned off";
        return ResponseEntity.ok(ApiResponse.ok(message, shop));
    }

    /** "sub" claim of the verified token = our user id. */
    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
