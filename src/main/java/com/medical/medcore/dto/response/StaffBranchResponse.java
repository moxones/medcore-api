package com.medical.medcore.dto.response;

import java.time.LocalDateTime;

public record StaffBranchResponse(
        Long id,
        Long userId,
        String userFullName,
        String userEmail,
        Long branchId,
        String branchName,
        String branchAddress,
        Boolean isActive,
        LocalDateTime createdAt
) {}
