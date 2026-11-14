package com.medical.medcore.clinical.dto;

import java.time.LocalDateTime;

public record OrderResultRequest(
        String result,
        String fileUrl,
        LocalDateTime resultDate,
        String status
) {}
