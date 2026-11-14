package com.medical.medcore.branch.dto;

import com.medical.medcore.branch.domain.Branch;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record BranchResponse(
        Long id,
        String name,
        String address,
        String ruc,
        String phone,
        LocalTime openingTime,
        LocalTime closingTime,
        Integer appointmentDurationMinutes,
        Boolean isActive,
        LocalDateTime createdAt
) {

    public static BranchResponse from(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getName(),
                branch.getAddress(),
                branch.getRuc(),
                branch.getPhone(),
                branch.getOpeningTime(),
                branch.getClosingTime(),
                branch.getAppointmentDurationMinutes(),
                branch.getIsActive(),
                branch.getCreatedAt());
    }
}
