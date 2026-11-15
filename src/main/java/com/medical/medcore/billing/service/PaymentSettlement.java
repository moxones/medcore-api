package com.medical.medcore.billing.service;

import com.medical.medcore.appointment.domain.Appointment;
import com.medical.medcore.appointment.service.AppointmentSettlement;
import com.medical.medcore.billing.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class PaymentSettlement implements AppointmentSettlement {

    private final PaymentRepository paymentRepository;

    @Override
    public boolean hasPendingBalance(Appointment appointment) {
        BigDecimal price = appointment.getPrice();
        if (price == null || price.signum() <= 0) {
            return false;
        }
        return paymentRepository.sumCompletedByAppointment(appointment.getId()).compareTo(price) < 0;
    }
}
