package com.medical.medcore.branch.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkBranchesRequest(
        @NotEmpty List<Long> branchIds
) {}
