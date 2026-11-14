package com.medical.medcore.doctor.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DoctorProfileResponse(
        Long doctorId,
        Long personId,
        String fullName,
        String initials,
        String email,
        String phone,
        String documentNumber,
        String licenseNumber,
        Boolean isActive,
        int seniorityYears,
        LocalDateTime joinedAt,
        List<String> specialties,
        List<String> branches,
        long totalPatients,
        long appointmentsThisMonth,
        long consultationsCompleted,
        long avgConsultationMinutes
) {}
