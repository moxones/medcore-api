package com.medical.medcore.billing.service;

import com.medical.medcore.appointment.domain.Appointment;
import com.medical.medcore.appointment.domain.AppointmentFlowStatus;
import com.medical.medcore.appointment.domain.AppointmentStatusCode;
import com.medical.medcore.appointment.service.AppointmentService;
import com.medical.medcore.appointment.service.AppointmentStatuses;
import com.medical.medcore.appointment.service.BookingPolicy;
import com.medical.medcore.billing.domain.Payment;
import com.medical.medcore.billing.domain.PaymentMethod;
import com.medical.medcore.billing.domain.PaymentStatus;
import com.medical.medcore.billing.dto.AppointmentBalanceResponse;
import com.medical.medcore.billing.dto.CreatePaymentRequest;
import com.medical.medcore.billing.dto.PaymentResponse;
import com.medical.medcore.billing.repository.PaymentRepository;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private static final String CODE_NOT_BILLABLE = "APPOINTMENT_NOT_BILLABLE";
    private static final String CODE_OVERPAYMENT = "PAYMENT_EXCEEDS_BALANCE";
    private static final String PAYMENT_NOT_FOUND = "Pago no encontrado";

    private final PaymentRepository paymentRepository;
    private final AppointmentService appointmentService;
    private final AppointmentStatuses statuses;
    private final BookingPolicy bookingPolicy;

    @Transactional
    public PaymentResponse register(Long appointmentId, CreatePaymentRequest request) {
        Appointment appointment = appointmentService.loadAccessible(appointmentId);
        assertBillable(appointment);
        PaymentStatus status = request.status() == PaymentStatus.PENDING ? PaymentStatus.PENDING : PaymentStatus.COMPLETED;
        if (status == PaymentStatus.COMPLETED) {
            assertWithinBalance(appointment, request.amount());
        }
        LocalDateTime now = bookingPolicy.now();
        Payment payment = paymentRepository.save(Payment.builder()
                .tenantId(appointment.getTenantId())
                .appointment(appointment)
                .amount(request.amount())
                .status(status)
                .paymentMethod(request.paymentMethod() != null ? request.paymentMethod() : PaymentMethod.CASH)
                .concept(clean(request.concept()))
                .reference(clean(request.reference()))
                .paymentDate(status == PaymentStatus.COMPLETED ? now : null)
                .build());
        closeAppointmentIfSettled(appointment, status);
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse confirm(Long appointmentId, Long paymentId) {
        Payment payment = loadPayment(appointmentId, paymentId);
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new ConflictException("PAYMENT_NOT_PENDING", "Solo se pueden confirmar pagos pendientes");
        }
        Appointment appointment = payment.getAppointment();
        assertWithinBalance(appointment, payment.getAmount());
        payment.confirm(bookingPolicy.now());
        paymentRepository.save(payment);
        closeAppointmentIfSettled(appointment, PaymentStatus.COMPLETED);
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse voidPayment(Long appointmentId, Long paymentId, String reason) {
        Payment payment = loadPayment(appointmentId, paymentId);
        if (payment.getStatus() == PaymentStatus.VOIDED) {
            throw new ConflictException("PAYMENT_ALREADY_VOIDED", "El pago ya fue anulado");
        }
        payment.voidPayment(reason.trim(), TenantContext.getCurrentUserId(), bookingPolicy.now());
        return toResponse(paymentRepository.save(payment));
    }

    public AppointmentBalanceResponse balance(Long appointmentId) {
        Appointment appointment = appointmentService.loadAccessible(appointmentId);
        BigDecimal paid = paymentRepository.sumCompletedByAppointment(appointment.getId());
        BigDecimal price = appointment.getPrice();
        BigDecimal pending = price == null ? null : price.subtract(paid).max(BigDecimal.ZERO);
        return new AppointmentBalanceResponse(appointment.getId(), price, paid, pending, list(appointment));
    }

    public List<PaymentResponse> listByAppointment(Long appointmentId) {
        return list(appointmentService.loadAccessible(appointmentId));
    }

    private List<PaymentResponse> list(Appointment appointment) {
        return paymentRepository.findByAppointmentIdOrderByIdDesc(appointment.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private Payment loadPayment(Long appointmentId, Long paymentId) {
        Appointment appointment = appointmentService.loadAccessible(appointmentId);
        return paymentRepository.findByIdAndTenantId(paymentId, appointment.getTenantId())
                .filter(payment -> payment.getAppointment().getId().equals(appointment.getId()))
                .orElseThrow(() -> new NotFoundException(PAYMENT_NOT_FOUND));
    }

    private void assertBillable(Appointment appointment) {
        boolean inactive = statuses.is(appointment.getStatusId(), AppointmentStatusCode.CANCELLED)
                || statuses.is(appointment.getStatusId(), AppointmentStatusCode.NO_SHOW);
        if (inactive) {
            throw new ConflictException(CODE_NOT_BILLABLE, "No se pueden registrar pagos en una cita cancelada o sin asistencia");
        }
    }

    private void assertWithinBalance(Appointment appointment, BigDecimal amount) {
        if (appointment.getPrice() == null) {
            return;
        }
        BigDecimal paid = paymentRepository.sumCompletedByAppointment(appointment.getId());
        if (paid.add(amount).compareTo(appointment.getPrice()) > 0) {
            throw new ConflictException(CODE_OVERPAYMENT,
                    "El pago supera el saldo pendiente de la cita (" + appointment.getPrice().subtract(paid).max(BigDecimal.ZERO) + ")");
        }
    }

    private void closeAppointmentIfSettled(Appointment appointment, PaymentStatus status) {
        if (status != PaymentStatus.COMPLETED || appointment.flow() != AppointmentFlowStatus.PENDING_PAYMENT) {
            return;
        }
        BigDecimal paid = paymentRepository.sumCompletedByAppointment(appointment.getId());
        boolean settled = appointment.getPrice() == null || paid.compareTo(appointment.getPrice()) >= 0;
        if (settled) {
            appointmentService.applyFlow(appointment, AppointmentFlowStatus.COMPLETED, "Pago registrado");
        }
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAppointment().getId(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null,
                payment.getConcept(),
                payment.getReference(),
                payment.getPaymentDate(),
                payment.getCreatedAt(),
                payment.getCreatedBy(),
                payment.getVoidedAt(),
                payment.getVoidReason(),
                payment.getAppointment().getFlowStatus());
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
