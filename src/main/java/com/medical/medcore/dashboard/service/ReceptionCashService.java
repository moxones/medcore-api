package com.medical.medcore.dashboard.service;

import com.medical.medcore.appointment.service.BookingPolicy;
import com.medical.medcore.billing.repository.PaymentRepository;
import com.medical.medcore.dashboard.dto.CashMethodTotal;
import com.medical.medcore.dashboard.dto.ReceptionCashResponse;
import com.medical.medcore.shared.security.BranchScope;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceptionCashService {

    private final PaymentRepository paymentRepository;
    private final BookingPolicy bookingPolicy;

    public ReceptionCashResponse today(Long branchId) {
        BranchScope scope = BranchScope.resolve(branchId);
        if (scope.seesNothing()) {
            return new ReceptionCashResponse(BigDecimal.ZERO, 0, List.of(), BigDecimal.ZERO, 0);
        }
        Long tenantId = TenantContext.requireTenantId();
        LocalDate today = bookingPolicy.today();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        List<CashMethodTotal> byMethod = paymentRepository
                .collectedByMethod(tenantId, from, to, scope.restricted(), scope.queryIds()).stream()
                .map(row -> new CashMethodTotal(String.valueOf(row[0]), asLong(row[1]), asBig(row[2])))
                .toList();
        BigDecimal collected = byMethod.stream().map(CashMethodTotal::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long operations = byMethod.stream().mapToLong(CashMethodTotal::operations).sum();

        Object[] pending = firstRow(paymentRepository.pendingBalance(tenantId, from, to, scope.restricted(), scope.queryIds()));
        return new ReceptionCashResponse(collected, operations, byMethod, asBig(pending[1]), asLong(pending[0]));
    }

    private static Object[] firstRow(List<Object[]> rows) {
        return rows.isEmpty() ? new Object[]{0L, BigDecimal.ZERO} : rows.get(0);
    }

    private static long asLong(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private static BigDecimal asBig(Object value) {
        if (value instanceof BigDecimal b) {
            return b;
        }
        return value instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO;
    }
}
