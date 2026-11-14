package com.medical.medcore.identity.service;

import java.time.LocalDate;

public record PersonData(
        String firstName,
        String lastName,
        LocalDate birthDate,
        String gender,
        String phone,
        String contactEmail,
        String address,
        String district,
        String documentTypeCode,
        String documentNumber
) {

    public boolean hasDocument() {
        return documentTypeCode != null && !documentTypeCode.isBlank()
                && documentNumber != null && !documentNumber.isBlank();
    }
}
