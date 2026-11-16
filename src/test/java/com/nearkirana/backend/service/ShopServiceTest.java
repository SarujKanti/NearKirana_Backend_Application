package com.nearkirana.backend.service;

import com.nearkirana.backend.dto.shop.ShopRequest;
import com.nearkirana.backend.dto.shop.ShopResponse;
import com.nearkirana.backend.dto.shop.ShopUpdateRequest;
import com.nearkirana.backend.entity.Role;
import com.nearkirana.backend.entity.Shop;
import com.nearkirana.backend.entity.User;
import com.nearkirana.backend.exception.ApiException;
import com.nearkirana.backend.repository.ShopRepository;
import com.nearkirana.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopServiceTest {

    private static final Long OWNER_ID = 5L;

    private ShopRepository shopRepository;
    private UserRepository userRepository;
    private ShopService service;
    private User shopkeeper;

    @BeforeEach
    void setUp() {
        shopRepository = mock(ShopRepository.class);
        userRepository = mock(UserRepository.class);
        service = new ShopService(shopRepository, userRepository);

        shopkeeper = new User("+919191919191", Role.SHOPKEEPER);
        ReflectionTestUtils.setField(shopkeeper, "id", OWNER_ID);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(shopkeeper));
        when(shopRepository.save(any(Shop.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shopRepository.saveAndFlush(any(Shop.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createShopUsesOwnerFromTokenAndCleansInput() {
        ShopResponse shop = service.createShop(OWNER_ID, request(" Ramu Kirana ", "98765 43210", true, new BigDecimal("3")));

        ArgumentCaptor<Shop> saved = ArgumentCaptor.forClass(Shop.class);
        verify(shopRepository).save(saved.capture());
        assertEquals(OWNER_ID, saved.getValue().getOwnerId());
        assertEquals("Ramu Kirana", shop.name());              // trimmed
        assertEquals("+919876543210", shop.contactNumber());   // normalized
        assertEquals(17.385, shop.latitude());                 // Y
        assertEquals(78.4867, shop.longitude());               // X
        assertEquals(new BigDecimal("3"), shop.deliveryRadiusKm());
        assertNull(shop.description());                        // blank -> null
    }

    @Test
    void customerCannotCreateShop() {
        User customer = new User("+919911111111", Role.CUSTOMER);
        when(userRepository.findById(4L)).thenReturn(Optional.of(customer));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.createShop(4L, request("Shop", "9876543210", false, null)));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(shopRepository, never()).save(any());
    }

    @Test
    void disabledShopkeeperIsRejected() {
        ReflectionTestUtils.setField(shopkeeper, "active", false);
        ApiException ex = assertThrows(ApiException.class, () -> service.getMyShops(OWNER_ID));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void invalidContactNumberIsRejected() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createShop(OWNER_ID, request("Shop", "12345", false, null)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void deliveryNeedsRadius() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createShop(OWNER_ID, request("Shop", "9876543210", true, null)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void radiusIsDroppedWhenNoDelivery() {
        ShopResponse shop = service.createShop(OWNER_ID, request("Shop", "9876543210", false, new BigDecimal("4")));
        assertFalse(shop.deliveryAvailable());
        assertNull(shop.deliveryRadiusKm());
    }

    @Test
    void shopLimitPerOwner() {
        when(shopRepository.countByOwnerId(OWNER_ID)).thenReturn((long) ShopService.MAX_SHOPS_PER_OWNER);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createShop(OWNER_ID, request("Shop", "9876543210", false, null)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void someoneElsesShopIsNotFound() {
        when(shopRepository.findByIdAndOwnerId(99L, OWNER_ID)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> service.getMyShop(OWNER_ID, 99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void updateAndCloseOwnShop() {
        Shop existing = new Shop(OWNER_ID);
        existing.updateDetails("Old", null, "+919876543210", "Main road", "Village", null,
                "Telangana", "500001", 17.0, 78.0, false, null);
        when(shopRepository.findByIdAndOwnerId(1L, OWNER_ID)).thenReturn(Optional.of(existing));

        ShopResponse updated = service.updateShop(OWNER_ID, 1L, request("New name", "9876543210", false, null));
        assertEquals("New name", updated.name());
        assertEquals(17.385, updated.latitude());

        ShopResponse closed = service.updateOpenStatus(OWNER_ID, 1L, false);
        assertFalse(closed.isOpen());
    }

    @Test
    void patchChangesOnlySentFields() {
        Shop existing = existingShop(true, new BigDecimal("3"));

        ShopResponse patched = service.patchShop(OWNER_ID, 1L,
                patch("Ramu Kirana Store 2", null, null, null, null));

        assertEquals("Ramu Kirana Store 2", patched.name());   // changed
        assertEquals("Daily groceries", patched.description()); // unchanged
        assertEquals("+919876543210", patched.contactNumber());
        assertEquals("501218", patched.pincode());
        assertEquals(17.2403, patched.latitude());
        assertEquals(new BigDecimal("3"), patched.deliveryRadiusKm());
        assertEquals(existing.isOpen(), patched.isOpen());
    }

    @Test
    void patchCanCloseShopAndClearDescription() {
        existingShop(false, null);
        ShopResponse patched = service.patchShop(OWNER_ID, 1L, patch(null, "", false, null, null));
        assertNull(patched.description());
        assertFalse(patched.isOpen());
    }

    @Test
    void patchTurningOnDeliveryNeedsRadius() {
        existingShop(false, null);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.patchShop(OWNER_ID, 1L, patch(null, null, null, true, null)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());

        ShopResponse ok = service.patchShop(OWNER_ID, 1L, patch(null, null, null, true, new BigDecimal("2.5")));
        assertEquals(new BigDecimal("2.5"), ok.deliveryRadiusKm());
    }

    @Test
    void emptyPatchIsRejected() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.patchShop(OWNER_ID, 1L, patch(null, null, null, null, null)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    private Shop existingShop(boolean delivery, BigDecimal radius) {
        Shop shop = new Shop(OWNER_ID);
        shop.updateDetails("Ramu Kirana Store", "Daily groceries", "+919876543210", "Near bus stand",
                "Shamshabad", "Rangareddy", "Telangana", "501218", 17.2403, 78.4294, delivery, radius);
        when(shopRepository.findByIdAndOwnerId(1L, OWNER_ID)).thenReturn(Optional.of(shop));
        return shop;
    }

    private static ShopUpdateRequest patch(String name, String description, Boolean isOpen,
                                           Boolean delivery, BigDecimal radius) {
        return new ShopUpdateRequest(name, description, null, null, null, null, null, null,
                null, null, isOpen, delivery, radius);
    }

    private static ShopRequest request(String name, String contact, boolean delivery, BigDecimal radius) {
        return new ShopRequest(name, "  ", contact, "Near bus stand", "Shamshabad", "Rangareddy",
                "Telangana", "501218", 17.385, 78.4867, delivery, radius);
    }
}
