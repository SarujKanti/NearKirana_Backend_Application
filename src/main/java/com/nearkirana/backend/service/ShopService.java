package com.nearkirana.backend.service;

import com.nearkirana.backend.dto.shop.ShopRequest;
import com.nearkirana.backend.dto.shop.ShopResponse;
import com.nearkirana.backend.entity.Role;
import com.nearkirana.backend.entity.Shop;
import com.nearkirana.backend.exception.ApiException;
import com.nearkirana.backend.repository.ShopRepository;
import com.nearkirana.backend.repository.UserRepository;
import com.nearkirana.backend.util.MobileNumbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Shop profile management for SHOPKEEPERS.
 *
 * Every method receives ownerId from the verified JWT (never from the request),
 * and every query filters by it. So a shopkeeper can only ever see or change
 * their OWN shops. Someone else's shop id simply returns "Shop not found"
 * (we do not even reveal that it exists).
 */
@Service
public class ShopService {

    private static final Logger log = LoggerFactory.getLogger(ShopService.class);

    /** Protects against mistakes or abuse (thousands of fake shops). Can be raised later. */
    static final int MAX_SHOPS_PER_OWNER = 5;

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;

    public ShopService(ShopRepository shopRepository, UserRepository userRepository) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
    }

    // POST /api/v1/shopkeeper/shops
    @Transactional
    public ShopResponse createShop(Long ownerId, ShopRequest request) {
        requireActiveShopkeeper(ownerId);
        if (shopRepository.countByOwnerId(ownerId) >= MAX_SHOPS_PER_OWNER) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "You can register at most " + MAX_SHOPS_PER_OWNER + " shops.");
        }

        Shop shop = new Shop(ownerId);
        applyDetails(shop, request);
        shop = shopRepository.save(shop);
        log.info("Shop {} created by user {}", shop.getId(), ownerId);
        return ShopResponse.from(shop);
    }

    // GET /api/v1/shopkeeper/shops
    @Transactional(readOnly = true)
    public List<ShopResponse> getMyShops(Long ownerId) {
        requireActiveShopkeeper(ownerId);
        return shopRepository.findByOwnerIdOrderByCreatedAtAsc(ownerId).stream()
                .map(ShopResponse::from)
                .toList();
    }

    // GET /api/v1/shopkeeper/shops/{shopId}
    @Transactional(readOnly = true)
    public ShopResponse getMyShop(Long ownerId, Long shopId) {
        requireActiveShopkeeper(ownerId);
        return ShopResponse.from(findOwnShop(ownerId, shopId));
    }

    // PUT /api/v1/shopkeeper/shops/{shopId}
    @Transactional
    public ShopResponse updateShop(Long ownerId, Long shopId, ShopRequest request) {
        requireActiveShopkeeper(ownerId);
        Shop shop = findOwnShop(ownerId, shopId);
        applyDetails(shop, request);
        // The entity is "managed" in this transaction, so Hibernate would save it on
        // commit anyway. saveAndFlush writes it NOW, so @PreUpdate sets updatedAt
        // before we build the response.
        log.info("Shop {} updated by user {}", shopId, ownerId);
        return ShopResponse.from(shopRepository.saveAndFlush(shop));
    }

    // PATCH /api/v1/shopkeeper/shops/{shopId}/status
    @Transactional
    public ShopResponse updateOpenStatus(Long ownerId, Long shopId, boolean open) {
        requireActiveShopkeeper(ownerId);
        Shop shop = findOwnShop(ownerId, shopId);
        shop.setOpen(open);
        log.info("Shop {} is now {} (user {})", shopId, open ? "OPEN" : "CLOSED", ownerId);
        return ShopResponse.from(shopRepository.saveAndFlush(shop));
    }

    // ------------------------------------------------------------------

    /**
     * The URL rule already demands role SHOPKEEPER in the token. We still check
     * the DATABASE, because a token lives up to 15 minutes: if the account was
     * disabled meanwhile, it must stop working at once.
     */
    private void requireActiveShopkeeper(Long userId) {
        boolean ok = userRepository.findById(userId)
                .filter(user -> user.isActive() && user.getRole() == Role.SHOPKEEPER)
                .isPresent();
        if (!ok) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only an active shopkeeper account can manage shops.");
        }
    }

    private Shop findOwnShop(Long ownerId, Long shopId) {
        return shopRepository.findByIdAndOwnerId(shopId, ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Shop not found."));
    }

    /** Cleans and checks the request, then copies it into the entity. */
    private void applyDetails(Shop shop, ShopRequest request) {
        String contactNumber = MobileNumbers.normalize(request.contactNumber())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                        "Please enter a valid 10-digit Indian contact number."));

        boolean delivery = Boolean.TRUE.equals(request.deliveryAvailable());
        BigDecimal radius = request.deliveryRadiusKm();
        if (delivery && radius == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Delivery radius (km) is required when delivery is available.");
        }
        if (!delivery) {
            radius = null; // no delivery -> no radius
        }

        shop.updateDetails(
                request.name().trim(),
                blankToNull(request.description()),
                contactNumber,
                request.addressLine().trim(),
                request.villageOrCity().trim(),
                blankToNull(request.district()),
                request.state().trim(),
                request.pincode(),
                request.latitude(),
                request.longitude(),
                delivery,
                radius);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
