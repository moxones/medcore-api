package com.medical.medcore.doctor.dto;

import com.medical.medcore.clinical.dto.MedicalEntryResponse.OrderResultItem;

import java.time.LocalDateTime;
import java.util.List;

public record DoctorOrderResponse(
        Long id,
        String orderType,
        String description,
        String status,
        LocalDateTime requestedAt,
        List<OrderResultItem> results,
        Long entryId,
        Long appointmentId,
        Long patientId,
        String patientName,
        String patientInitials
) {}
