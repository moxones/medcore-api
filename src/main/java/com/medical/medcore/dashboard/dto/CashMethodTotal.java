package com.medical.medcore.dashboard.dto;

import java.math.BigDecimal;

public record CashMethodTotal(
        String method,
        long operations,
        BigDecimal amount
) {}
