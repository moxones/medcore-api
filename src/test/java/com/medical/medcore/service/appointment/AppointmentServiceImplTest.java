package com.medical.medcore.service.appointment;

import com.medical.medcore.config.exception.ConflictException;
import com.medical.medcore.dto.request.UpdateAppointmentFlowRequest;
import com.medical.medcore.dto.response.AppointmentResponse;
import com.medical.medcore.entity.Appointment;
import com.medical.medcore.entity.enums.AppointmentFlowStatus;
import com.medical.medcore.repository.AppointmentRepository;
import com.medical.medcore.repository.AppointmentRescheduleRepository;
import com.medical.medcore.repository.AppointmentTypeRepository;
import com.medical.medcore.repository.BranchRepository;
import com.medical.medcore.repository.DoctorBranchRepository;
import com.medical.medcore.repository.DoctorRepository;
import com.medical.medcore.repository.DoctorScheduleRepository;
import com.medical.medcore.repository.DoctorSpecialtyRepository;
import com.medical.medcore.repository.PatientRepository;
import com.medical.medcore.repository.UserRepository;
import com.medical.medcore.service.appointment.impl.AppointmentServiceImpl;
import com.medical.medcore.util.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppointmentServiceImplTest {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long APPOINTMENT_ID = 100L;

    private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Lima");
    // 2026-06-19T10:00 hora de Lima (UTC-5) -> hoy = 2026-06-19
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-19T15:00:00Z"), CLINIC_ZONE);

    private AppointmentRepository appointmentRepository;
    private AppointmentServiceImpl service;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        BookingPolicy bookingPolicy = new BookingPolicy(15, FIXED_CLOCK);

        service = new AppointmentServiceImpl(
                appointmentRepository,
                mock(AppointmentRescheduleRepository.class),
                mock(DoctorRepository.class),
                mock(DoctorBranchRepository.class),
                mock(DoctorScheduleRepository.class),
                mock(DoctorSpecialtyRepository.class),
                mock(PatientRepository.class),
                mock(BranchRepository.class),
                mock(AppointmentTypeRepository.class),
                mock(UserRepository.class),
                bookingPolicy);

        TenantContext.set(TENANT_ID, USER_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void updateFlowStatus_toWaiting_onTodaysAppointment_succeeds() {
        Appointment appointment = appointment(LocalDateTime.of(2026, 6, 19, 9, 0), AppointmentFlowStatus.SCHEDULED);
        stubRepository(appointment);

        AppointmentResponse response = service.updateFlowStatus(
                APPOINTMENT_ID, new UpdateAppointmentFlowRequest("WAITING"));

        assertNotNull(response);
        assertEquals(AppointmentFlowStatus.WAITING.name(), appointment.getFlowStatus());
        assertNotNull(appointment.getCheckedInAt());
    }

    @Test
    void updateFlowStatus_toWaiting_onTomorrowsAppointment_throwsConflict() {
        Appointment appointment = appointment(LocalDateTime.of(2026, 6, 20, 9, 0), AppointmentFlowStatus.SCHEDULED);
        stubRepository(appointment);

        ConflictException ex = assertThrows(ConflictException.class, () ->
                service.updateFlowStatus(APPOINTMENT_ID, new UpdateAppointmentFlowRequest("WAITING")));

        assertEquals("APPOINTMENT_NOT_TODAY", ex.getCode());
        assertEquals(AppointmentFlowStatus.SCHEDULED.name(), appointment.getFlowStatus());
    }

    @Test
    void updateFlowStatus_toWaiting_onYesterdaysAppointment_throwsConflict() {
        Appointment appointment = appointment(LocalDateTime.of(2026, 6, 18, 9, 0), AppointmentFlowStatus.SCHEDULED);
        stubRepository(appointment);

        ConflictException ex = assertThrows(ConflictException.class, () ->
                service.updateFlowStatus(APPOINTMENT_ID, new UpdateAppointmentFlowRequest("WAITING")));

        assertEquals("APPOINTMENT_NOT_TODAY", ex.getCode());
        assertEquals(AppointmentFlowStatus.SCHEDULED.name(), appointment.getFlowStatus());
    }

    @Test
    void updateFlowStatus_toCompleted_onYesterdaysInProgressAppointment_succeeds() {
        Appointment appointment = appointment(LocalDateTime.of(2026, 6, 18, 9, 0), AppointmentFlowStatus.IN_PROCESS);
        stubRepository(appointment);

        AppointmentResponse response = service.updateFlowStatus(
                APPOINTMENT_ID, new UpdateAppointmentFlowRequest("COMPLETED"));

        assertNotNull(response);
        assertEquals(AppointmentFlowStatus.COMPLETED.name(), appointment.getFlowStatus());
        assertNotNull(appointment.getCompletedAt());
    }

    private Appointment appointment(LocalDateTime scheduledAt, AppointmentFlowStatus flowStatus) {
        return Appointment.builder()
                .id(APPOINTMENT_ID)
                .tenantId(TENANT_ID)
                .scheduledAt(scheduledAt)
                .statusId(1L)
                .durationMinutes(30)
                .flowStatus(flowStatus.name())
                .build();
    }

    private void stubRepository(Appointment appointment) {
        when(appointmentRepository.findById(APPOINTMENT_ID)).thenReturn(Optional.of(appointment));
        lenient().when(appointmentRepository.findByIdWithDetails(eq(APPOINTMENT_ID), eq(TENANT_ID)))
                .thenReturn(Optional.of(appointment));
        lenient().when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
    }
}
