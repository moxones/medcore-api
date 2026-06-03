package com.medical.medcore.service.payment.impl;

import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.request.CreatePaymentRequest;
import com.medical.medcore.dto.response.PaymentResponse;
import com.medical.medcore.entity.Appointment;
import com.medical.medcore.entity.Payment;
import com.medical.medcore.repository.AppointmentRepository;
import com.medical.medcore.repository.PaymentRepository;
import com.medical.medcore.service.payment.PaymentService;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional
    public PaymentResponse register(Long appointmentId, CreatePaymentRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.getCurrentUserId();

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));
        if (!appointment.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("No tienes acceso a esta cita");
        }

        String status = (request.status() == null || request.status().isBlank())
                ? "COMPLETED" : request.status().trim().toUpperCase();

        Payment payment = Payment.builder()
                .appointment(appointment)
                .amount(request.amount())
                .status(status)
                .paymentMethod(request.paymentMethod())
                .concept(request.concept())
                .createdBy(userId)
                .paymentDate("COMPLETED".equals(status) ? LocalDateTime.now() : null)
                .build();

        payment = paymentRepository.save(payment);
        return mapToResponse(payment, appointmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> listByAppointment(Long appointmentId) {
        Long tenantId = TenantContext.requireTenantId();

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Cita no encontrada"));
        if (!appointment.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("No tienes acceso a esta cita");
        }

        return paymentRepository.findByAppointmentIdOrderByIdDesc(appointmentId).stream()
                .map(p -> mapToResponse(p, appointmentId))
                .toList();
    }

    private PaymentResponse mapToResponse(Payment p, Long appointmentId) {
        return new PaymentResponse(
                p.getId(),
                appointmentId,
                p.getAmount(),
                p.getStatus(),
                p.getPaymentMethod(),
                p.getConcept(),
                p.getPaymentDate(),
                p.getCreatedAt(),
                p.getCreatedBy()
        );
    }
}
