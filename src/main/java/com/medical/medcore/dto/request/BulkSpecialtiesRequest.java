package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkSpecialtiesRequest(
    @NotEmpty List<Long> specialtyIds
) {}