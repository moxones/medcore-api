package com.medical.medcore.service.payment;

import com.medical.medcore.dto.request.CreatePaymentRequest;
import com.medical.medcore.dto.response.PaymentResponse;

import java.util.List;

public interface PaymentService {

    PaymentResponse register(Long appointmentId, CreatePaymentRequest request);

    List<PaymentResponse> listByAppointment(Long appointmentId);
}
