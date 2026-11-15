package com.nearkirana.backend.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobileNumbersTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "9876543210", "+919876543210", "919876543210", "09876543210",
            "98765 43210", "+91 98765-43210", "(+91) 9876543210"
    })
    void acceptsCommonIndianFormats(String input) {
        assertEquals("+919876543210", MobileNumbers.normalize(input).orElseThrow());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "5876543210",     // Indian mobiles start with 6-9
            "987654321",      // 9 digits
            "98765432101",    // 11 digits
            "+449876543210",  // not India
            "98765abcde"
    })
    void rejectsInvalidNumbers(String input) {
        assertTrue(MobileNumbers.normalize(input).isEmpty());
    }

    @org.junit.jupiter.api.Test
    void masksAllButLastFourDigits() {
        assertEquals("+91******3210", MobileNumbers.mask("+919876543210"));
    }
}
