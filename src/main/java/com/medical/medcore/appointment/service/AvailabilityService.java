package com.medical.medcore.appointment.service;

import com.medical.medcore.appointment.domain.Appointment;
import com.medical.medcore.appointment.dto.AvailabilitySlotResponse;
import com.medical.medcore.appointment.dto.DayAvailabilityResponse;
import com.medical.medcore.appointment.dto.SpecialtySummaryResponse;
import com.medical.medcore.appointment.dto.TimeSlotResponse;
import com.medical.medcore.appointment.repository.AppointmentRepository;
import com.medical.medcore.branch.repository.BranchRepository;
import com.medical.medcore.catalog.domain.AppointmentType;
import com.medical.medcore.catalog.domain.Specialty;
import com.medical.medcore.catalog.repository.AppointmentTypeRepository;
import com.medical.medcore.doctor.domain.Doctor;
import com.medical.medcore.doctor.domain.DoctorBranch;
import com.medical.medcore.doctor.domain.DoctorSchedule;
import com.medical.medcore.doctor.domain.DoctorSpecialty;
import com.medical.medcore.doctor.repository.DoctorBranchRepository;
import com.medical.medcore.doctor.repository.DoctorRepository;
import com.medical.medcore.doctor.repository.DoctorScheduleRepository;
import com.medical.medcore.doctor.repository.DoctorSpecialtyRepository;
import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int DEFAULT_SLOT_MINUTES = 30;
    private static final int MAX_RANGE_DAYS = 62;
    private static final int SUMMARY_HORIZON_DAYS = 30;

    private final BranchRepository branchRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorBranchRepository doctorBranchRepository;
    private final DoctorSpecialtyRepository doctorSpecialtyRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentTypeRepository appointmentTypeRepository;
    private final AppointmentStatuses statuses;
    private final BookingPolicy bookingPolicy;

    public List<TimeSlotResponse> getAvailableSlots(Long doctorId, Long branchId, LocalDate date) {
        Long tenantId = TenantContext.requireTenantId();
        requireBranch(branchId, tenantId);
        doctorRepository.findByIdAndTenantId(doctorId, tenantId)
                .orElseThrow(() -> new NotFoundException("Doctor no encontrado"));

        List<DoctorSchedule> schedules = activeOn(
                doctorScheduleRepository.findActiveByDoctorIdAndBranchIdAndDay(doctorId, branchId, dayIndex(date)), date);
        if (schedules.isEmpty()) {
            return List.of();
        }
        BusyCalendar busy = loadBusy(tenantId, List.of(doctorId), date, date);
        List<TimeSlotResponse> slots = new ArrayList<>();
        for (DoctorSchedule schedule : schedules) {
            int slotMinutes = slotMinutes(schedule, 0);
            for (LocalTime start : slotStarts(schedule, slotMinutes)) {
                if (!bookingPolicy.isBookable(date, start)) {
                    continue;
                }
                LocalTime end = start.plusMinutes(slotMinutes);
                slots.add(new TimeSlotResponse(start, end, busy.isFree(doctorId, date, start, end)));
            }
        }
        slots.sort(Comparator.comparing(TimeSlotResponse::startTime));
        return slots;
    }

    public List<DayAvailabilityResponse> getAvailability(Long branchId, LocalDate fromDate, LocalDate toDate,
                                                         Long specialtyId, Long doctorId, Long appointmentTypeId) {
        Long tenantId = TenantContext.requireTenantId();
        requireBranch(branchId, tenantId);
        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException("El rango de fechas es inválido");
        }
        if (ChronoUnit.DAYS.between(fromDate, toDate) > MAX_RANGE_DAYS) {
            throw new BadRequestException("El rango máximo de consulta es de " + MAX_RANGE_DAYS + " días");
        }

        Map<Long, Doctor> doctors = activeDoctorsInBranch(branchId, doctorId);
        if (doctors.isEmpty()) {
            return List.of();
        }
        Map<Long, String> specialtyNames = specialtyNames(new ArrayList<>(doctors.keySet()), specialtyId);
        if (specialtyId != null) {
            doctors.keySet().retainAll(specialtyNames.keySet());
            if (doctors.isEmpty()) {
                return List.of();
            }
        }
        List<Long> doctorIds = new ArrayList<>(doctors.keySet());
        int durationOverride = typeDuration(appointmentTypeId);
        ScheduleIndex schedules = new ScheduleIndex(
                doctorScheduleRepository.findActiveByDoctorIdInAndBranchId(doctorIds, branchId));
        BusyCalendar busy = loadBusy(tenantId, doctorIds, fromDate, toDate);

        List<DayAvailabilityResponse> result = new ArrayList<>();
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            List<AvailabilitySlotResponse> daySlots = new ArrayList<>();
            for (Long id : doctorIds) {
                Person person = doctors.get(id).getPerson();
                for (DoctorSchedule schedule : activeOn(schedules.of(id, dayIndex(date)), date)) {
                    int slotMinutes = slotMinutes(schedule, durationOverride);
                    for (LocalTime start : slotStarts(schedule, slotMinutes)) {
                        LocalTime end = start.plusMinutes(slotMinutes);
                        if (bookingPolicy.isBookable(date, start) && busy.isFree(id, date, start, end)) {
                            daySlots.add(new AvailabilitySlotResponse(
                                    start.format(TIME_FORMAT), end.format(TIME_FORMAT), id,
                                    doctorDisplayName(person), initials(person),
                                    specialtyNames.getOrDefault(id, ""), slotMinutes));
                        }
                    }
                }
            }
            if (!daySlots.isEmpty()) {
                daySlots.sort(Comparator.comparing(AvailabilitySlotResponse::startTime));
                result.add(new DayAvailabilityResponse(date.toString(), daySlots));
            }
        }
        return result;
    }

    public List<SpecialtySummaryResponse> getSpecialtiesSummary(Long branchId) {
        Long tenantId = TenantContext.requireTenantId();
        requireBranch(branchId, tenantId);
        Map<Long, Doctor> doctors = activeDoctorsInBranch(branchId, null);
        if (doctors.isEmpty()) {
            return List.of();
        }
        List<Long> doctorIds = new ArrayList<>(doctors.keySet());
        Map<Long, Specialty> specialties = new HashMap<>();
        Map<Long, Set<Long>> doctorsBySpecialty = new HashMap<>();
        for (DoctorSpecialty link : doctorSpecialtyRepository.findByDoctorIdIn(doctorIds)) {
            Long specialtyId = link.getSpecialty().getId();
            specialties.put(specialtyId, link.getSpecialty());
            doctorsBySpecialty.computeIfAbsent(specialtyId, key -> new HashSet<>()).add(link.getDoctor().getId());
        }
        LocalDate today = bookingPolicy.today();
        LocalDate horizon = today.plusDays(SUMMARY_HORIZON_DAYS);
        ScheduleIndex schedules = new ScheduleIndex(
                doctorScheduleRepository.findActiveByDoctorIdInAndBranchId(doctorIds, branchId));
        BusyCalendar busy = loadBusy(tenantId, doctorIds, today, horizon);

        List<SpecialtySummaryResponse> result = new ArrayList<>();
        doctorsBySpecialty.forEach((specialtyId, ids) -> {
            Specialty specialty = specialties.get(specialtyId);
            String nextDate = firstAvailableDate(ids, today, horizon, schedules, busy)
                    .map(LocalDate::toString)
                    .orElse(null);
            result.add(new SpecialtySummaryResponse(specialtyId, specialty.getCode(), specialty.getName(),
                    ids.size(), nextDate));
        });
        result.sort(Comparator.comparing(SpecialtySummaryResponse::nextAvailableDate,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    public int resolveDuration(Long doctorId, Long branchId, LocalDateTime start, int requestedMinutes) {
        DoctorSchedule window = requireWindow(doctorId, branchId, start, requestedMinutes);
        return requestedMinutes > 0 ? requestedMinutes : slotMinutes(window, 0);
    }

    private DoctorSchedule requireWindow(Long doctorId, Long branchId, LocalDateTime start, int requestedMinutes) {
        LocalDate date = start.toLocalDate();
        LocalTime time = start.toLocalTime();
        return activeOn(doctorScheduleRepository.findActiveByDoctorIdAndBranchIdAndDay(doctorId, branchId, dayIndex(date)), date)
                .stream()
                .filter(schedule -> fits(schedule, time, requestedMinutes > 0 ? requestedMinutes : slotMinutes(schedule, 0)))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("OUTSIDE_SCHEDULE",
                        "El horario seleccionado está fuera del horario de atención del médico en esta sucursal"));
    }

    private boolean fits(DoctorSchedule schedule, LocalTime start, int minutes) {
        long startOffset = ChronoUnit.MINUTES.between(schedule.getStartTime(), start);
        long available = ChronoUnit.MINUTES.between(start, schedule.getEndTime());
        return startOffset >= 0 && available >= minutes;
    }

    private Optional<LocalDate> firstAvailableDate(Set<Long> doctorIds, LocalDate from, LocalDate to,
                                                   ScheduleIndex schedules, BusyCalendar busy) {
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            for (Long doctorId : doctorIds) {
                for (DoctorSchedule schedule : activeOn(schedules.of(doctorId, dayIndex(date)), date)) {
                    int slotMinutes = slotMinutes(schedule, 0);
                    for (LocalTime start : slotStarts(schedule, slotMinutes)) {
                        if (bookingPolicy.isBookable(date, start)
                                && busy.isFree(doctorId, date, start, start.plusMinutes(slotMinutes))) {
                            return Optional.of(date);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Map<Long, Doctor> activeDoctorsInBranch(Long branchId, Long onlyDoctorId) {
        Map<Long, Doctor> doctors = new HashMap<>();
        for (DoctorBranch link : doctorBranchRepository.findActiveDoctorsByBranchId(branchId)) {
            Doctor doctor = link.getDoctor();
            boolean matches = onlyDoctorId == null || onlyDoctorId.equals(doctor.getId());
            if (matches && Boolean.TRUE.equals(doctor.getIsActive())) {
                doctors.put(doctor.getId(), doctor);
            }
        }
        return doctors;
    }

    private Map<Long, String> specialtyNames(List<Long> doctorIds, Long specialtyId) {
        Map<Long, String> names = new HashMap<>();
        for (DoctorSpecialty link : doctorSpecialtyRepository.findByDoctorIdIn(doctorIds)) {
            if (specialtyId == null || link.getSpecialty().getId().equals(specialtyId)) {
                names.putIfAbsent(link.getDoctor().getId(), link.getSpecialty().getName());
            }
        }
        return names;
    }

    private BusyCalendar loadBusy(Long tenantId, List<Long> doctorIds, LocalDate from, LocalDate to) {
        List<Appointment> existing = appointmentRepository.findByDoctorsAndDateRange(
                tenantId, doctorIds, from.atStartOfDay(), to.plusDays(1).atStartOfDay(), statuses.inactiveIds());
        return new BusyCalendar(existing);
    }

    private List<DoctorSchedule> activeOn(List<DoctorSchedule> schedules, LocalDate date) {
        return schedules.stream()
                .filter(schedule -> schedule.getValidFrom() == null || !date.isBefore(schedule.getValidFrom()))
                .filter(schedule -> schedule.getValidUntil() == null || !date.isAfter(schedule.getValidUntil()))
                .toList();
    }

    private List<LocalTime> slotStarts(DoctorSchedule schedule, int slotMinutes) {
        List<LocalTime> starts = new ArrayList<>();
        long windowMinutes = ChronoUnit.MINUTES.between(schedule.getStartTime(), schedule.getEndTime());
        for (long offset = 0; offset + slotMinutes <= windowMinutes; offset += slotMinutes) {
            starts.add(schedule.getStartTime().plusMinutes(offset));
        }
        return starts;
    }

    private int slotMinutes(DoctorSchedule schedule, int override) {
        if (override > 0) {
            return override;
        }
        Integer configured = schedule.getSlotDurationMinutes();
        return configured != null && configured > 0 ? configured : DEFAULT_SLOT_MINUTES;
    }

    private int typeDuration(Long appointmentTypeId) {
        if (appointmentTypeId == null) {
            return 0;
        }
        return appointmentTypeRepository.findById(appointmentTypeId)
                .map(AppointmentType::getDurationMinutes)
                .filter(minutes -> minutes != null && minutes > 0)
                .orElse(0);
    }

    private void requireBranch(Long branchId, Long tenantId) {
        branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new NotFoundException("Sucursal no encontrada"));
    }

    private int dayIndex(LocalDate date) {
        return date.getDayOfWeek().getValue() - 1;
    }

    private String doctorDisplayName(Person person) {
        String prefix = "F".equalsIgnoreCase(person.getGender()) ? "Dra." : "Dr.";
        return prefix + " " + person.fullName();
    }

    private String initials(Person person) {
        return initial(person.getFirstName()) + initial(person.getLastName());
    }

    private String initial(String value) {
        return value == null || value.isBlank() ? "" : value.trim().substring(0, 1).toUpperCase();
    }

    private static final class ScheduleIndex {

        private final Map<Long, Map<Integer, List<DoctorSchedule>>> byDoctorAndDay = new HashMap<>();

        private ScheduleIndex(List<DoctorSchedule> schedules) {
            for (DoctorSchedule schedule : schedules) {
                byDoctorAndDay
                        .computeIfAbsent(schedule.getDoctor().getId(), key -> new HashMap<>())
                        .computeIfAbsent(schedule.getDayOfWeek(), key -> new ArrayList<>())
                        .add(schedule);
            }
        }

        private List<DoctorSchedule> of(Long doctorId, int dayOfWeek) {
            return byDoctorAndDay.getOrDefault(doctorId, Map.of()).getOrDefault(dayOfWeek, List.of());
        }
    }

    private static final class BusyCalendar {

        private final Map<Long, Map<LocalDate, List<LocalTime[]>>> intervals = new HashMap<>();

        private BusyCalendar(List<Appointment> appointments) {
            for (Appointment appointment : appointments) {
                LocalTime start = appointment.getScheduledAt().toLocalTime();
                LocalTime end = appointment.endsAt().toLocalTime();
                intervals
                        .computeIfAbsent(appointment.getDoctor().getId(), key -> new HashMap<>())
                        .computeIfAbsent(appointment.getScheduledAt().toLocalDate(), key -> new ArrayList<>())
                        .add(new LocalTime[]{start, end});
            }
        }

        private boolean isFree(Long doctorId, LocalDate date, LocalTime start, LocalTime end) {
            return intervals.getOrDefault(doctorId, Map.of()).getOrDefault(date, List.of()).stream()
                    .noneMatch(busy -> busy[0].isBefore(end) && start.isBefore(busy[1]));
        }
    }
}
