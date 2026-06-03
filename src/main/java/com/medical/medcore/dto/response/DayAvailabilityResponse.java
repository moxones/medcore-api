package com.medical.medcore.dto.response;

import java.util.List;

public record DayAvailabilityResponse(
        String date,
        List<AvailabilitySlotResponse> slots
) {}
