package com.medical.medcore.dto.request;

import java.time.LocalDateTime;

public record OrderResultRequest(
        String result,
        String fileUrl,
        LocalDateTime resultDate,
        String status
) {}
