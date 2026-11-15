package com.medical.medcore.schedule.dto;

import com.medical.medcore.schedule.domain.AffectedResolution;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BranchClosureRequest(
        Long branchId,
        @NotNull(message = "La fecha es obligatoria") LocalDate date,
        @NotBlank(message = "El nombre del cierre es obligatorio") @Size(max = 120) String name,
        AffectedResolution resolution,
        @Size(max = 255) String cancelMessage
) {
}
