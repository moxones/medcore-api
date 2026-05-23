package com.medical.medcore.service.doctor;

import com.medical.medcore.config.exception.BadRequestException;
import com.medical.medcore.dto.response.DoctorBranchScheduleResponse;
import com.medical.medcore.dto.response.DoctorCardResponse;
import com.medical.medcore.entity.*;
import com.medical.medcore.repository.AppointmentRepository;
import com.medical.medcore.repository.DoctorBranchRepository;
import com.medical.medcore.repository.DoctorRepository;
import com.medical.medcore.repository.DoctorScheduleRepository;
import com.medical.medcore.repository.DoctorSpecialtyRepository;
import com.medical.medcore.types.PageableResponse;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorListService {

    private final DoctorRepository doctorRepository;
    private final DoctorSpecialtyRepository doctorSpecialtyRepository;
    private final DoctorBranchRepository doctorBranchRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public PageableResponse<DoctorCardResponse> findCards(
            Long branchId, Long specialtyId, Boolean isActive, Boolean availableToday,
            int page, int size) {

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BadRequestException("Tenant no disponible");
        }

        LocalDate today = LocalDate.now();
        Integer todayDayOfWeek = Boolean.TRUE.equals(availableToday)
                ? (today.getDayOfWeek().getValue() - 1)
                : null;

        Page<Doctor> doctorsPage = doctorRepository.findByFiltersForCard(
                tenantId, isActive, branchId, specialtyId,
                todayDayOfWeek, today,
                PageRequest.of(page, size));

        if (doctorsPage.isEmpty()) {
            return PageableResponse.from(doctorsPage.map(d ->
                    new DoctorCardResponse(null, null, null, null, null, null, null,
                            List.of(), List.of(), false, Map.of(), 0L, 0)));
        }

        List<Long> doctorIds = doctorsPage.getContent().stream()
                .map(Doctor::getId).toList();

        // Batch load specialties
        Map<Long, List<String>> specialtiesByDoctor = doctorSpecialtyRepository
                .findByDoctorIdIn(doctorIds).stream()
                .collect(Collectors.groupingBy(
                        ds -> ds.getDoctor().getId(),
                        Collectors.mapping(ds -> ds.getSpecialty().getName(), Collectors.toList())));

        // Batch load branches
        Map<Long, List<String>> branchesByDoctor = doctorBranchRepository
                .findActiveBranchesByDoctorIdIn(doctorIds).stream()
                .collect(Collectors.groupingBy(
                        db -> db.getDoctor().getId(),
                        Collectors.mapping(db -> db.getBranch().getName(), Collectors.toList())));

        // Batch load schedules
        Map<Long, List<DoctorSchedule>> schedulesByDoctor = doctorScheduleRepository
                .findActiveByDoctorIdIn(doctorIds).stream()
                .collect(Collectors.groupingBy(s -> s.getDoctor().getId()));

        // Batch load appointment counts (this month and previous month)
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDateTime startOfMonth = firstOfMonth.atStartOfDay();
        LocalDateTime startOfNextMonth = firstOfMonth.plusMonths(1).atStartOfDay();
        LocalDateTime startOfPrevMonth = firstOfMonth.minusMonths(1).atStartOfDay();

        Map<Long, Long> thisMonthCounts = toCountMap(appointmentRepository
                .countByDoctorIdInAndPeriod(tenantId, doctorIds, startOfMonth, startOfNextMonth));

        Map<Long, Long> prevMonthCounts = toCountMap(appointmentRepository
                .countByDoctorIdInAndPeriod(tenantId, doctorIds, startOfPrevMonth, startOfMonth));

        return PageableResponse.from(doctorsPage.map(doctor -> buildCard(
                doctor, specialtiesByDoctor, branchesByDoctor, schedulesByDoctor,
                thisMonthCounts, prevMonthCounts, today)));
    }

    private DoctorCardResponse buildCard(
            Doctor doctor,
            Map<Long, List<String>> specialtiesByDoctor,
            Map<Long, List<String>> branchesByDoctor,
            Map<Long, List<DoctorSchedule>> schedulesByDoctor,
            Map<Long, Long> thisMonthCounts,
            Map<Long, Long> prevMonthCounts,
            LocalDate today) {

        Long doctorId = doctor.getId();
        String firstName = doctor.getPerson().getFirstName();
        String lastName = doctor.getPerson().getLastName();
        String fullName = "Dr. " + (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
        String initials = initial(firstName) + initial(lastName);

        int seniorityYears = doctor.getCreatedAt() != null
                ? (int) ChronoUnit.YEARS.between(doctor.getCreatedAt().toLocalDate(), today)
                : 0;

        List<String> specialties = specialtiesByDoctor.getOrDefault(doctorId, List.of());
        List<String> branches = branchesByDoctor.getOrDefault(doctorId, List.of());
        List<DoctorSchedule> schedules = schedulesByDoctor.getOrDefault(doctorId, List.of());

        // 0=Monday...6=Sunday (Java DayOfWeek.getValue() is 1=Mon → subtract 1)
        int todayDow = today.getDayOfWeek().getValue() - 1;

        boolean availableToday = schedules.stream().anyMatch(s ->
                s.getDayOfWeek() != null && s.getDayOfWeek() == todayDow
                && (s.getValidFrom() == null || !s.getValidFrom().isAfter(today))
                && (s.getValidUntil() == null || !s.getValidUntil().isBefore(today)));

        Map<String, DoctorBranchScheduleResponse> weekScheduleByBranch = buildWeekScheduleByBranch(schedules, branchesByDoctor.getOrDefault(doctorId, List.of()));

        long thisMonth = thisMonthCounts.getOrDefault(doctorId, 0L);
        long prevMonth = prevMonthCounts.getOrDefault(doctorId, 0L);
        int growthPercent = prevMonth == 0
                ? (thisMonth > 0 ? 100 : 0)
                : (int) Math.round(((double) (thisMonth - prevMonth) / prevMonth) * 100);

        return new DoctorCardResponse(
                doctorId,
                doctor.getPerson() != null ? doctor.getPerson().getId() : null,
                fullName.strip(),
                initials,
                doctor.getLicenseNumber(),
                doctor.getIsActive(),
                seniorityYears,
                specialties,
                branches,
                availableToday,
                weekScheduleByBranch,
                thisMonth,
                growthPercent
        );
    }

    private Map<String, DoctorBranchScheduleResponse> buildWeekScheduleByBranch(
            List<DoctorSchedule> schedules, List<String> branchNames) {

        Map<String, DoctorBranchScheduleResponse> result = new LinkedHashMap<>();

        for (String branchName : branchNames) {
            Map<String, String> scheduleByDay = new LinkedHashMap<>();

            for (int i = 0; i <= 6; i++) {
                final int dow = i;
                String daySchedule = schedules.stream()
                        .filter(s -> s.getDayOfWeek() != null && s.getDayOfWeek() == dow)
                        .filter(s -> {
                            String branchNameFromDirect = s.getBranch() != null ? s.getBranch().getName() : null;
                            String branchNameFromDoctorBranch = s.getDoctorBranch() != null && s.getDoctorBranch().getBranch() != null 
                                ? s.getDoctorBranch().getBranch().getName() : null;
                            String actualBranch = (branchNameFromDirect != null) ? branchNameFromDirect : branchNameFromDoctorBranch;
                            return branchName != null && branchName.equals(actualBranch);
                        })
                        .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                        .map(s -> s.getStartTime() + "-" + s.getEndTime())
                        .collect(Collectors.joining(", "));

                scheduleByDay.put(String.valueOf(i), daySchedule.isEmpty() ? null : daySchedule);
            }

            result.put(branchName, new DoctorBranchScheduleResponse(null, branchName, scheduleByDay));
        }

        return result;
    }

    private static String initial(String name) {
        return (name != null && !name.isEmpty()) ? String.valueOf(name.charAt(0)).toUpperCase() : "";
    }

    private static Map<Long, Long> toCountMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> (Long) row[1]));
    }
}
