package com.medical.medcore.platform.dto;

import com.medical.medcore.platform.domain.BusinessType;

public record ProfessionalTermsResponse(
        String singular,
        String plural,
        String singularLabel,
        String pluralLabel,
        String title,
        String titleFemale
) {

    public static ProfessionalTermsResponse from(BusinessType type) {
        return new ProfessionalTermsResponse(type.professional(), type.professionals(), type.professionalLabel(),
                type.professionalsLabel(), type.title(), type.titleFemale());
    }
}
