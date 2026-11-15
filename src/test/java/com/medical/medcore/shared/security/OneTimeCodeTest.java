package com.medical.medcore.shared.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OneTimeCodeTest {

    @Test
    void generatesSixDigitCodesKeepingLeadingZeros() {
        for (int i = 0; i < 200; i++) {
            assertThat(OneTimeCode.generate()).matches("\\d{6}");
        }
    }

    @Test
    void hashIgnoresSpacesAndSeparatorsTypedByThePerson() {
        assertThat(OneTimeCode.hash("user:1", "482 913")).isEqualTo(OneTimeCode.hash("user:1", "482913"));
        assertThat(OneTimeCode.hash("user:1", "482-913")).isEqualTo(OneTimeCode.hash("user:1", "482913"));
    }

    @Test
    void theSameCodeHashesDifferentlyForDifferentOwners() {
        assertThat(OneTimeCode.hash("user:1", "123456")).isNotEqualTo(OneTimeCode.hash("user:2", "123456"));
    }
}
