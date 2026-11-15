package com.medical.medcore.appointment.service;

import com.medical.medcore.appointment.domain.Appointment;
import com.medical.medcore.appointment.domain.CareStage;
import com.medical.medcore.appointment.dto.AppointmentResponse;
import com.medical.medcore.branch.domain.Branch;
import com.medical.medcore.catalog.domain.AppointmentType;
import com.medical.medcore.catalog.repository.AppointmentTypeRepository;
import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.platform.domain.ClinicProcess;
import com.medical.medcore.platform.service.TenantProcessConfigService;
import com.medical.medcore.triage.repository.TriageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AppointmentMapper {

    private final TriageRepository triageRepository;
    private final TenantProcessConfigService processConfigService;
    private final AppointmentTypeRepository appointmentTypeRepository;
    private final AppointmentStatuses statuses;

    public AppointmentResponse toResponse(Appointment appointment) {
        return toResponses(List.of(appointment)).get(0);
    }

    public List<AppointmentResponse> toResponses(List<Appointment> appointments) {
        if (appointments.isEmpty()) {
            return List.of();
        }
        boolean triageEnabled = Boolean.TRUE.equals(processConfigService.getConfig().get(ClinicProcess.TRIAGE));
        Set<Long> triaged = triageEnabled
                ? new HashSet<>(triageRepository.findAppointmentIdsWithTriage(
                        appointments.stream().map(Appointment::getId).toList()))
                : Set.of();
        Map<Long, String> typeNames = appointmentTypeRepository.findAll().stream()
                .collect(Collectors.toMap(AppointmentType::getId, AppointmentType::getName));
        return appointments.stream()
                .map(appointment -> map(appointment, triageEnabled, triaged.contains(appointment.getId()), typeNames))
                .toList();
    }

    private Integer ageOf(LocalDate birthDate) {
        LocalDate today = LocalDate.now();
        if (birthDate == null || birthDate.isAfter(today)) {
            return null;
        }
        return Period.between(birthDate, today).getYears();
    }

    private AppointmentResponse map(Appointment appointment, boolean triageEnabled, boolean triageCompleted,
                                    Map<Long, String> typeNames) {
        Person patient = appointment.getPatient().getPerson();
        Person doctor = appointment.getDoctor().getPerson();
        Branch branch = appointment.getBranch();
        CareStage careStage = CareStage.resolve(appointment.flow(), triageEnabled, triageCompleted);
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatient().getId(),
                patient.fullName(),
                patient.getPhone(),
                ageOf(patient.getBirthDate()),
                patient.getGender(),
                appointment.getPatient().getMedicalRecordNumber(),
                appointment.getDoctor().getId(),
                doctor.fullName(),
                branch != null ? branch.getId() : null,
                branch != null ? branch.getName() : null,
                appointment.getScheduledAt(),
                appointment.getStatusId(),
                statuses.codeOf(appointment.getStatusId()),
                appointment.getAppointmentTypeId(),
                appointment.getAppointmentTypeId() != null ? typeNames.get(appointment.getAppointmentTypeId()) : null,
                appointment.getReason(),
                appointment.getDurationMinutes(),
                appointment.getFlowStatus(),
                careStage.name(),
                triageEnabled ? triageCompleted : null,
                appointment.getCreatedAt(),
                appointment.getBookingSource() != null ? appointment.getBookingSource().name() : null,
                appointment.getCheckedInAt(),
                appointment.getCalledAt(),
                appointment.getStartedAt(),
                appointment.getFinishedAt(),
                appointment.getCompletedAt(),
                appointment.getPrice(),
                appointment.getCancelReason(),
                appointment.getCancelledAt(),
                appointment.getNoShowAt(),
                appointment.isFlaggedForReschedule(),
                appointment.getRescheduleCause() != null ? appointment.getRescheduleCause().name() : null,
                appointment.getRescheduleNote(),
                appointment.getRescheduleFlaggedAt(),
                appointment.getCancellationOrigin() != null ? appointment.getCancellationOrigin().name() : null);
    }
}
