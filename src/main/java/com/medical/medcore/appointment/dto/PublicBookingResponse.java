package com.medical.medcore.appointment.dto;

import com.medical.medcore.identity.dto.AuthResponse;

public record PublicBookingResponse(AuthResponse session, AppointmentResponse appointment) {
}
