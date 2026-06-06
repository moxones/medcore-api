package com.medical.medcore.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DoctorPatientResponse(
        Long patientId,
        String fullName,
        String initials,
        String gender,
        LocalDate birthDate,
        Integer age,
        String phone,
        String email,
        String bloodType,
        long allergyCount,
        long conditionCount,
        long totalVisits,
        LocalDateTime lastVisitAt,
        String lastReason,
        LocalDateTime nextAppointmentAt
) {}
