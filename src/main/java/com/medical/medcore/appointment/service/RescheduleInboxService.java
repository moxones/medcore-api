package com.medical.medcore.appointment.service;

import com.medical.medcore.appointment.domain.Appointment;
import com.medical.medcore.appointment.domain.AppointmentStatusCode;
import com.medical.medcore.appointment.dto.AppointmentResponse;
import com.medical.medcore.appointment.dto.AvailabilitySlotResponse;
import com.medical.medcore.appointment.dto.DayAvailabilityResponse;
import com.medical.medcore.appointment.dto.RescheduleInboxSummaryResponse;
import com.medical.medcore.appointment.dto.RescheduleSuggestionResponse;
import com.medical.medcore.appointment.dto.RescheduleSuggestionsResponse;
import com.medical.medcore.appointment.repository.AppointmentRepository;
import com.medical.medcore.doctor.domain.DoctorSpecialty;
import com.medical.medcore.doctor.repository.DoctorSpecialtyRepository;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.security.BranchScope;
import com.medical.medcore.shared.security.SecurityUtils;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RescheduleInboxService {

    private static final int SAME_PROFESSIONAL_DAYS = 21;
    private static final int OTHER_PROFESSIONAL_DAYS = 14;
    private static final int SAME_PROFESSIONAL_LIMIT = 8;
    private static final int OTHER_PROFESSIONAL_LIMIT = 6;

    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;
    private final AppointmentAccess access;
    private final AppointmentMapper mapper;
    private final AppointmentStatuses statuses;
    private final AvailabilityService availabilityService;
    private final DoctorSpecialtyRepository doctorSpecialtyRepository;
    private final BookingPolicy bookingPolicy;

    public List<AppointmentResponse> list(Long branchId, Long doctorId) {
        List<Appointment> flagged = load(branchId, doctorId);
        return mapper.toResponses(flagged);
    }

    public RescheduleInboxSummaryResponse summary(Long branchId) {
        List<Appointment> flagged = load(branchId, null);
        LocalDate today = bookingPolicy.today();
        LocalDateTime now = bookingPolicy.now();
        long todayCount = flagged.stream()
                .filter(appointment -> appointment.getScheduledAt().toLocalDate().equals(today))
                .count();
        long overdue = flagged.stream().filter(appointment -> appointment.getScheduledAt().isBefore(now)).count();
        return new RescheduleInboxSummaryResponse(flagged.size(), todayCount, overdue);
    }

    @Transactional
    public AppointmentResponse dismiss(Long appointmentId) {
        Appointment appointment = appointmentService.loadAccessible(appointmentId);
        appointmentService.assertScheduled(appointment);
        if (!appointment.isFlaggedForReschedule()) {
            throw new ConflictException("NOT_FLAGGED", "La cita no está pendiente de reprogramar");
        }
        appointment.resolveRescheduleFlag();
        return mapper.toResponse(appointmentRepository.saveAndFlush(appointment));
    }

    public RescheduleSuggestionsResponse suggestions(Long appointmentId) {
        Appointment appointment = appointmentService.loadAccessible(appointmentId);
        appointmentService.assertScheduled(appointment);
        Long branchId = appointment.getBranch().getId();
        Long doctorId = appointment.getDoctor().getId();
        int duration = appointment.getDurationMinutes();
        LocalDate today = bookingPolicy.today();

        List<RescheduleSuggestionResponse> same = flatten(availabilityService.freeSlots(branchId, today,
                today.plusDays(SAME_PROFESSIONAL_DAYS - 1), null, doctorId, duration), appointment);
        same.sort(Comparator.comparing((RescheduleSuggestionResponse slot) -> !slot.sameWeek())
                .thenComparing(RescheduleSuggestionResponse::date)
                .thenComparing(RescheduleSuggestionResponse::startTime));

        List<RescheduleSuggestionResponse> others = new ArrayList<>();
        if (SecurityUtils.isStaff() && !SecurityUtils.isDoctorOnly()) {
            Map<String, RescheduleSuggestionResponse> unique = new LinkedHashMap<>();
            for (DoctorSpecialty link : doctorSpecialtyRepository.findByDoctorIdFetchSpecialty(doctorId)) {
                for (RescheduleSuggestionResponse slot : flatten(availabilityService.freeSlots(branchId, today,
                        today.plusDays(OTHER_PROFESSIONAL_DAYS - 1), link.getSpecialty().getId(), null, duration),
                        appointment)) {
                    if (!slot.doctorId().equals(doctorId)) {
                        unique.putIfAbsent(slot.doctorId() + "|" + slot.date() + "|" + slot.startTime(), slot);
                    }
                }
            }
            others.addAll(unique.values());
            others.sort(Comparator.comparing(RescheduleSuggestionResponse::date)
                    .thenComparing(RescheduleSuggestionResponse::startTime));
        }
        return new RescheduleSuggestionsResponse(appointment.getId(),
                same.stream().limit(SAME_PROFESSIONAL_LIMIT).toList(),
                others.stream().limit(OTHER_PROFESSIONAL_LIMIT).toList());
    }

    private List<Appointment> load(Long branchId, Long doctorId) {
        BranchScope scope = BranchScope.resolve(branchId);
        if (scope.seesNothing()) {
            return List.of();
        }
        return appointmentRepository.findRescheduleInbox(TenantContext.requireTenantId(),
                statuses.idOf(AppointmentStatusCode.SCHEDULED), access.restrictDoctorFilter(doctorId),
                scope.restricted(), scope.queryIds());
    }

    private List<RescheduleSuggestionResponse> flatten(List<DayAvailabilityResponse> days, Appointment appointment) {
        LocalDate original = appointment.getScheduledAt().toLocalDate();
        String originalTime = appointment.getScheduledAt().toLocalTime().toString();
        int originalWeek = original.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int originalYear = original.get(IsoFields.WEEK_BASED_YEAR);
        List<RescheduleSuggestionResponse> result = new ArrayList<>();
        for (DayAvailabilityResponse day : days) {
            LocalDate date = LocalDate.parse(day.date());
            boolean sameWeek = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) == originalWeek
                    && date.get(IsoFields.WEEK_BASED_YEAR) == originalYear;
            for (AvailabilitySlotResponse slot : day.slots()) {
                result.add(new RescheduleSuggestionResponse(day.date(), slot.startTime(), slot.endTime(),
                        slot.doctorId(), slot.doctorName(), sameWeek, originalTime.startsWith(slot.startTime())));
            }
        }
        return result;
    }
}
