package com.medical.medcore.controller;

import com.medical.medcore.dto.request.CreatePaymentRequest;
import com.medical.medcore.dto.response.PaymentResponse;
import com.medical.medcore.security.authorization.annotation.RequireStaff;
import com.medical.medcore.service.payment.PaymentService;
import com.medical.medcore.types.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/appointments/{appointmentId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @RequireStaff
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> register(
            @PathVariable Long appointmentId,
            @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                paymentService.register(appointmentId, request), "Pago registrado"));
    }

    @RequireStaff
    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> list(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                paymentService.listByAppointment(appointmentId), "Pagos de la cita"));
    }
}
