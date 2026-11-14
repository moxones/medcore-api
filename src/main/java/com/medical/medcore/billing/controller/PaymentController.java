package com.medical.medcore.billing.controller;

import com.medical.medcore.billing.dto.AppointmentBalanceResponse;
import com.medical.medcore.billing.dto.CreatePaymentRequest;
import com.medical.medcore.billing.dto.PaymentResponse;
import com.medical.medcore.billing.dto.VoidPaymentRequest;
import com.medical.medcore.billing.service.PaymentService;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.security.annotation.RequireAdminOrSuperAdmin;
import com.medical.medcore.shared.security.annotation.RequireReception;
import com.medical.medcore.shared.security.annotation.RequireStaff;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/appointments/{appointmentId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @RequireReception
    @PostMapping
    public ApiResponse<PaymentResponse> register(@PathVariable Long appointmentId,
                                                 @Valid @RequestBody CreatePaymentRequest request) {
        return ApiResponse.ok(paymentService.register(appointmentId, request), "Pago registrado");
    }

    @RequireStaff
    @GetMapping
    public ApiResponse<List<PaymentResponse>> list(@PathVariable Long appointmentId) {
        return ApiResponse.ok(paymentService.listByAppointment(appointmentId), "Pagos de la cita");
    }

    @RequireStaff
    @GetMapping("/balance")
    public ApiResponse<AppointmentBalanceResponse> balance(@PathVariable Long appointmentId) {
        return ApiResponse.ok(paymentService.balance(appointmentId), "Saldo de la cita");
    }

    @RequireReception
    @PostMapping("/{paymentId}/confirm")
    public ApiResponse<PaymentResponse> confirm(@PathVariable Long appointmentId, @PathVariable Long paymentId) {
        return ApiResponse.ok(paymentService.confirm(appointmentId, paymentId), "Pago confirmado");
    }

    @RequireAdminOrSuperAdmin
    @PostMapping("/{paymentId}/void")
    public ApiResponse<PaymentResponse> voidPayment(@PathVariable Long appointmentId,
                                                    @PathVariable Long paymentId,
                                                    @Valid @RequestBody VoidPaymentRequest request) {
        return ApiResponse.ok(paymentService.voidPayment(appointmentId, paymentId, request.reason()), "Pago anulado");
    }
}
