package com.medical.medcore.doctor.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkSpecialtiesRequest(
    @NotEmpty List<Long> specialtyIds
) {}