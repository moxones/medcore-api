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
import com.medical.medcore.platform.service.TenantTerminology;
import com.medical.medcore.doctor.domain.Doctor;
import com.medical.medcore.doctor.domain.DoctorBranch;
import com.medical.medcore.doctor.domain.DoctorSpecialty;
import com.medical.medcore.doctor.repository.DoctorBranchRepository;
import com.medical.medcore.doctor.repository.DoctorRepository;
import com.medical.medcore.doctor.repository.DoctorSpecialtyRepository;
import com.medical.medcore.identity.domain.Person;
import com.medical.medcore.schedule.domain.ScheduleException;
import com.medical.medcore.schedule.service.WorkWindow;
import com.medical.medcore.schedule.service.WorkingCalendar;
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
    private static final int MAX_RANGE_DAYS = 62;
    private static final int SUMMARY_HORIZON_DAYS = 30;

    private final BranchRepository branchRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorBranchRepository doctorBranchRepository;
    private final DoctorSpecialtyRepository doctorSpecialtyRepository;
    private final WorkingCalendar workingCalendar;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentTypeRepository appointmentTypeRepository;
    private final AppointmentStatuses statuses;
    private final BookingPolicy bookingPolicy;
    private final TenantTerminology terminology;

    public List<TimeSlotResponse> getAvailableSlots(Long doctorId, Long branchId, LocalDate date) {
        Long tenantId = TenantContext.requireTenantId();
        requireBranch(branchId, tenantId);
        doctorRepository.findByIdAndTenantId(doctorId, tenantId)
                .orElseThrow(() -> new NotFoundException("Doctor no encontrado"));

        List<WorkWindow> windows = workingCalendar.windows(doctorId, branchId, date);
        if (windows.isEmpty()) {
            return List.of();
        }
        BusyCalendar busy = loadBusy(tenantId, List.of(doctorId), date, date);
        List<TimeSlotResponse> slots = new ArrayList<>();
        for (WorkWindow window : windows) {
            int slotMinutes = slotMinutes(window, 0);
            for (LocalTime start : slotStarts(window, slotMinutes)) {
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
        return freeSlots(branchId, fromDate, toDate, specialtyId, doctorId, typeDuration(appointmentTypeId));
    }

    public List<DayAvailabilityResponse> freeSlots(Long branchId, LocalDate fromDate, LocalDate toDate,
                                                   Long specialtyId, Long doctorId, int durationOverride) {
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
        WorkingCalendar.Snapshot calendar = workingCalendar.load(doctorIds, branchId, fromDate, toDate);
        BusyCalendar busy = loadBusy(tenantId, doctorIds, fromDate, toDate);

        List<DayAvailabilityResponse> result = new ArrayList<>();
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            List<AvailabilitySlotResponse> daySlots = new ArrayList<>();
            for (Long id : doctorIds) {
                Person person = doctors.get(id).getPerson();
                for (WorkWindow window : calendar.windows(id, date)) {
                    int slotMinutes = slotMinutes(window, durationOverride);
                    for (LocalTime start : slotStarts(window, slotMinutes)) {
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
        WorkingCalendar.Snapshot calendar = workingCalendar.load(doctorIds, branchId, today, horizon);
        BusyCalendar busy = loadBusy(tenantId, doctorIds, today, horizon);

        List<SpecialtySummaryResponse> result = new ArrayList<>();
        doctorsBySpecialty.forEach((specialtyId, ids) -> {
            Specialty specialty = specialties.get(specialtyId);
            String nextDate = firstAvailableDate(ids, today, horizon, calendar, busy)
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
        WorkWindow window = requireWindow(doctorId, branchId, start, requestedMinutes);
        return requestedMinutes > 0 ? requestedMinutes : slotMinutes(window, 0);
    }

    private WorkWindow requireWindow(Long doctorId, Long branchId, LocalDateTime start, int requestedMinutes) {
        LocalDate date = start.toLocalDate();
        LocalTime time = start.toLocalTime();
        WorkingCalendar.Snapshot calendar = workingCalendar.load(List.of(doctorId), branchId, date, date);
        return calendar.windows(doctorId, date).stream()
                .filter(window -> window.fits(time, requestedMinutes > 0 ? requestedMinutes : slotMinutes(window, 0)))
                .findFirst()
                .orElseThrow(() -> outsideSchedule(calendar, doctorId, date, time));
    }

    private BadRequestException outsideSchedule(WorkingCalendar.Snapshot calendar, Long doctorId, LocalDate date,
                                                LocalTime time) {
        boolean inTemplate = calendar.templateWindows(doctorId, date).stream()
                .anyMatch(window -> !time.isBefore(window.start()) && time.isBefore(window.end()));
        if (inTemplate && calendar.closure(date).isPresent()) {
            return new BadRequestException("BRANCH_CLOSED",
                    "La sede no atiende el " + date + " (" + calendar.closure(date).get().getName() + ")");
        }
        boolean blocked = calendar.exceptions(doctorId, date).stream()
                .filter(ScheduleException::isBlock)
                .anyMatch(exception -> exception.isFullDay()
                        || (!time.isBefore(exception.getStartTime()) && time.isBefore(exception.getEndTime())));
        if (inTemplate && blocked) {
            return new BadRequestException("PROFESSIONAL_UNAVAILABLE",
                    "El profesional registró una ausencia en ese horario");
        }
        return new BadRequestException("OUTSIDE_SCHEDULE",
                "El horario seleccionado está fuera del horario de atención del profesional en esta sucursal");
    }

    private Optional<LocalDate> firstAvailableDate(Set<Long> doctorIds, LocalDate from, LocalDate to,
                                                   WorkingCalendar.Snapshot calendar, BusyCalendar busy) {
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            for (Long doctorId : doctorIds) {
                for (WorkWindow window : calendar.windows(doctorId, date)) {
                    int slotMinutes = slotMinutes(window, 0);
                    for (LocalTime start : slotStarts(window, slotMinutes)) {
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

    private List<LocalTime> slotStarts(WorkWindow window, int slotMinutes) {
        List<LocalTime> starts = new ArrayList<>();
        long windowMinutes = window.minutes();
        for (long offset = 0; offset + slotMinutes <= windowMinutes; offset += slotMinutes) {
            starts.add(window.start().plusMinutes(offset));
        }
        return starts;
    }

    private int slotMinutes(WorkWindow window, int override) {
        return override > 0 ? override : window.slotMinutes();
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

    private String doctorDisplayName(Person person) {
        return terminology.professionalName(person);
    }

    private String initials(Person person) {
        return initial(person.getFirstName()) + initial(person.getLastName());
    }

    private String initial(String value) {
        return value == null || value.isBlank() ? "" : value.trim().substring(0, 1).toUpperCase();
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
