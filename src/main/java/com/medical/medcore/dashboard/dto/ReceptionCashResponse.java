package com.medical.medcore.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReceptionCashResponse(
        BigDecimal collected,
        long operations,
        List<CashMethodTotal> byMethod,
        BigDecimal pendingBalance,
        long pendingCount
) {}
