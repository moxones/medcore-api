package com.medical.medcore.platform.dto;

import com.medical.medcore.platform.domain.BusinessType;
import com.medical.medcore.platform.domain.ClinicModule;

import java.util.List;

public record BusinessTypeResponse(
        BusinessType code,
        String label,
        ProfessionalTermsResponse terms,
        List<ClinicModule> defaultModules
) {

    public static BusinessTypeResponse from(BusinessType type) {
        return new BusinessTypeResponse(type, type.label(), ProfessionalTermsResponse.from(type),
                type.defaultModules().stream().sorted().toList());
    }
}
