package com.medical.medcore.schedule.dto;

import com.medical.medcore.schedule.domain.AffectedResolution;
import jakarta.validation.constraints.Size;

public record ResolutionRequest(
        AffectedResolution resolution,
        @Size(max = 255) String message
) {
}
