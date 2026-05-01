package com.medical.medcore.dto.response;

import java.time.LocalTime;

public record TimeSlotResponse(
        LocalTime startTime,
        LocalTime endTime,
        boolean isAvailable
) {}
