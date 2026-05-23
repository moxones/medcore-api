package com.medical.medcore.dto.response;

import java.time.LocalDateTime;

public record DoctorBranchResponse(
        Long id,
        Long doctorId,
        String doctorFullName,
        String doctorLicenseNumber,
        Long branchId,
        String branchName,
        String branchAddress,
        Boolean isActive,
        LocalDateTime createdAt
) {}
