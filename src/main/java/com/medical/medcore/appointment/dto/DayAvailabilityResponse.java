package com.medical.medcore.appointment.dto;

import java.util.List;

public record DayAvailabilityResponse(
        String date,
        List<AvailabilitySlotResponse> slots
) {}
