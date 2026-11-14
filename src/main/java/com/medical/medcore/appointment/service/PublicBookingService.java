package com.medical.medcore.appointment.service;

import com.medical.medcore.appointment.domain.Appointment;
import com.medical.medcore.appointment.domain.BookingSource;
import com.medical.medcore.appointment.dto.PublicBookingRequest;
import com.medical.medcore.appointment.dto.PublicBookingResponse;
import com.medical.medcore.identity.dto.AuthResponse;
import com.medical.medcore.patient.domain.Patient;
import com.medical.medcore.patient.dto.PatientSelfRegistrationRequest;
import com.medical.medcore.patient.service.PatientRegistrationService;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicBookingService {

    private final PatientRegistrationService registrationService;
    private final AppointmentService appointmentService;
    private final AppointmentMapper mapper;

    @Transactional
    public PublicBookingResponse registerAndBook(PublicBookingRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        PatientRegistrationService.Registration registration = registrationService.register(
                new PatientSelfRegistrationRequest(request.firstName(), request.lastName(), request.birthDate(),
                        request.phone(), request.documentTypeCode(), request.documentNumber(),
                        request.email(), request.password()));
        Patient patient = registration.patient();
        AuthResponse session = registration.session();
        Appointment appointment = appointmentService.book(tenantId, patient, request.doctorId(), request.branchId(),
                request.scheduledAt(), request.appointmentTypeId(), request.reason(), BookingSource.PUBLIC_WEB, false);
        return new PublicBookingResponse(session, mapper.toResponse(appointment));
    }
}
