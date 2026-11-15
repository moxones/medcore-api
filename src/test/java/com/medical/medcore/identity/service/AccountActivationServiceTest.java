package com.medical.medcore.identity.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountActivationServiceTest {

    @Test
    void masksTheEmailKeepingTheFirstLetterAndTheDomain() {
        assertThat(AccountActivationService.maskEmail("walter.paz@correo.pe")).isEqualTo("w******@correo.pe");
        assertThat(AccountActivationService.maskEmail("ana@x.pe")).isEqualTo("a**@x.pe");
        assertThat(AccountActivationService.maskEmail("a@x.pe")).isEqualTo("a@x.pe");
    }
}
