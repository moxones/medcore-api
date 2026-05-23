package com.medical.medcore.service.doctor;

import com.medical.medcore.config.exception.BadRequestException;
import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.request.DoctorScheduleRequest;
import com.medical.medcore.dto.request.DoctorScheduleUpdateRequest;
import com.medical.medcore.dto.response.DoctorScheduleResponse;
import com.medical.medcore.entity.Branch;
import com.medical.medcore.entity.Doctor;
import com.medical.medcore.entity.DoctorBranch;
import com.medical.medcore.entity.DoctorSchedule;
import com.medical.medcore.repository.DoctorBranchRepository;
import com.medical.medcore.repository.DoctorRepository;
import com.medical.medcore.repository.DoctorScheduleRepository;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorScheduleService {

    private final DoctorRepository doctorRepository;
    private final DoctorBranchRepository doctorBranchRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;

    // -------------------------------------------------------------------------
    // GET list
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<DoctorScheduleResponse> listForDoctor(
            Long doctorId, Long branchId, Integer dayOfWeek, Boolean isActive) {

        requireDoctorInTenant(doctorId);

        return doctorScheduleRepository
                .findByDoctorIdWithFilters(doctorId, dayOfWeek, isActive)
                .stream()
                .filter(s -> branchId == null || matchesBranch(s, branchId))
                .map(this::toResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // GET detail
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public DoctorScheduleResponse findById(Long doctorId, Long scheduleId) {
        requireDoctorInTenant(doctorId);
        return toResponse(requireScheduleForDoctor(doctorId, scheduleId));
    }

    // -------------------------------------------------------------------------
    // POST create
    // -------------------------------------------------------------------------

    @Transactional
    public DoctorScheduleResponse create(Long doctorId, DoctorScheduleRequest request) {
        Doctor doctor = requireDoctorInTenant(doctorId);
        Long tenantId = TenantContext.requireTenantId();

        DoctorBranch doctorBranch = loadAndValidateDoctorBranch(
                request.doctorBranchId(), doctorId, tenantId);

        validateTimes(request.startTime(), request.endTime());
        validateDateRange(request.validFrom(), request.validUntil());
        validateNoOverlap(request.doctorBranchId(), request.dayOfWeek(),
                request.startTime(), request.endTime(), null);

        DoctorSchedule schedule = DoctorSchedule.builder()
                .doctor(doctor)
                .doctorBranch(doctorBranch)
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .slotDurationMinutes(request.slotDurationMinutes())
                .maxPatientsPerSlot(request.maxPatientsPerSlot())
                .validFrom(request.validFrom())
                .validUntil(request.validUntil())
                .isActive(request.isActive() != null ? request.isActive() : true)
                .createdBy(TenantContext.requireCurrentUserId())
                .build();

        schedule = doctorScheduleRepository.save(schedule);
        return toResponseWithDoctorBranch(schedule, doctorBranch);
    }

    // -------------------------------------------------------------------------
    // PUT update
    // -------------------------------------------------------------------------

    @Transactional
    public DoctorScheduleResponse update(Long doctorId, Long scheduleId,
                                         DoctorScheduleUpdateRequest request) {
        requireDoctorInTenant(doctorId);
        Long tenantId = TenantContext.requireTenantId();

        DoctorSchedule schedule = requireScheduleForDoctor(doctorId, scheduleId);

        // Resolve effective doctorBranch
        DoctorBranch effectiveDoctorBranch = schedule.getDoctorBranch();
        if (request.doctorBranchId() != null) {
            Long currentBranchId = effectiveDoctorBranch != null ? effectiveDoctorBranch.getId() : null;
            if (!request.doctorBranchId().equals(currentBranchId)) {
                effectiveDoctorBranch = loadAndValidateDoctorBranch(
                        request.doctorBranchId(), doctorId, tenantId);
            }
        }

        // Resolve effective field values
        Integer effectiveDayOfWeek = firstNonNull(request.dayOfWeek(), schedule.getDayOfWeek());
        LocalTime effectiveStart   = firstNonNull(request.startTime(), schedule.getStartTime());
        LocalTime effectiveEnd     = firstNonNull(request.endTime(), schedule.getEndTime());
        LocalDate effectiveFrom    = firstNonNull(request.validFrom(), schedule.getValidFrom());
        LocalDate effectiveUntil   = firstNonNull(request.validUntil(), schedule.getValidUntil());

        validateTimes(effectiveStart, effectiveEnd);
        validateDateRange(effectiveFrom, effectiveUntil);

        Long effectiveDoctorBranchId = effectiveDoctorBranch != null ? effectiveDoctorBranch.getId() : null;
        if (effectiveDoctorBranchId != null) {
            validateNoOverlap(effectiveDoctorBranchId, effectiveDayOfWeek,
                    effectiveStart, effectiveEnd, scheduleId);
        }

        // Apply changes
        if (request.doctorBranchId() != null)      schedule.setDoctorBranch(effectiveDoctorBranch);
        if (request.dayOfWeek() != null)            schedule.setDayOfWeek(effectiveDayOfWeek);
        if (request.startTime() != null)            schedule.setStartTime(effectiveStart);
        if (request.endTime() != null)              schedule.setEndTime(effectiveEnd);
        if (request.slotDurationMinutes() != null)  schedule.setSlotDurationMinutes(request.slotDurationMinutes());
        if (request.maxPatientsPerSlot() != null)   schedule.setMaxPatientsPerSlot(request.maxPatientsPerSlot());
        if (request.validFrom() != null)            schedule.setValidFrom(effectiveFrom);
        if (request.validUntil() != null)           schedule.setValidUntil(effectiveUntil);
        if (request.isActive() != null)             schedule.setIsActive(request.isActive());
        schedule.setUpdatedBy(TenantContext.requireCurrentUserId());

        return toResponse(doctorScheduleRepository.save(schedule));
    }

    // -------------------------------------------------------------------------
    // DELETE soft-delete
    // -------------------------------------------------------------------------

    @Transactional
    public void deactivate(Long doctorId, Long scheduleId) {
        requireDoctorInTenant(doctorId);
        DoctorSchedule schedule = requireScheduleForDoctor(doctorId, scheduleId);
        schedule.setIsActive(false);
        schedule.setUpdatedBy(TenantContext.requireCurrentUserId());
        doctorScheduleRepository.save(schedule);
    }

    // -------------------------------------------------------------------------
    // Legacy helper (used by DoctorListService batch path, keep for compat)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<DoctorScheduleResponse> listForDoctorAndBranch(Long doctorId, Long branchId) {
        requireDoctorInTenant(doctorId);
        return doctorScheduleRepository.findActiveByDoctorIdAndBranchId(doctorId, branchId)
                .stream().map(this::toResponse).toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Doctor requireDoctorInTenant(Long doctorId) {
        Long tenantId = TenantContext.requireTenantId();
        return doctorRepository.findByIdAndTenantId(doctorId, tenantId)
                .orElseThrow(() -> new NotFoundException("Doctor no encontrado"));
    }

    private DoctorSchedule requireScheduleForDoctor(Long doctorId, Long scheduleId) {
        return doctorScheduleRepository.findByIdAndDoctorId(scheduleId, doctorId)
                .orElseThrow(() -> new NotFoundException("Horario no encontrado"));
    }

    private DoctorBranch loadAndValidateDoctorBranch(Long doctorBranchId, Long doctorId, Long tenantId) {
        DoctorBranch db = doctorBranchRepository.findByIdFetchAll(doctorBranchId)
                .orElseThrow(() -> new NotFoundException("Vinculación médico-sucursal no encontrada"));
        if (!db.getDoctor().getId().equals(doctorId)) {
            throw new NotFoundException("Vinculación médico-sucursal no encontrada");
        }
        if (!Boolean.TRUE.equals(db.getIsActive())) {
            throw new BadRequestException("El médico no está activo en esta sucursal");
        }
        if (!db.getBranch().getTenantId().equals(tenantId)) {
            throw new NotFoundException("Vinculación médico-sucursal no encontrada");
        }
        return db;
    }

    private void validateTimes(LocalTime start, LocalTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new BadRequestException("La hora de inicio debe ser anterior a la hora de fin");
        }
    }

    private void validateDateRange(LocalDate from, LocalDate until) {
        if (from != null && until != null && until.isBefore(from)) {
            throw new BadRequestException("valid_until debe ser mayor o igual a valid_from");
        }
    }

    private void validateNoOverlap(Long doctorBranchId, Integer dayOfWeek,
                                   LocalTime startTime, LocalTime endTime, Long excludeId) {
        if (doctorScheduleRepository.countOverlap(
                doctorBranchId, dayOfWeek, startTime, endTime, excludeId) > 0) {
            throw new BadRequestException(
                    "El horario se solapa con un horario activo existente para ese día");
        }
    }

    private boolean matchesBranch(DoctorSchedule s, Long branchId) {
        if (s.getDoctorBranch() != null && s.getDoctorBranch().getBranch() != null) {
            return branchId.equals(s.getDoctorBranch().getBranch().getId());
        }
        return s.getBranch() != null && branchId.equals(s.getBranch().getId());
    }

    private static <T> T firstNonNull(T a, T b) {
        return a != null ? a : b;
    }

    // -------------------------------------------------------------------------
    // toResponse
    // -------------------------------------------------------------------------

    private DoctorScheduleResponse toResponse(DoctorSchedule ds) {
        Long doctorBranchId = null;
        Long branchId       = null;
        String branchName   = null;

        if (ds.getDoctorBranch() != null) {
            doctorBranchId = ds.getDoctorBranch().getId();
            Branch b = ds.getDoctorBranch().getBranch();
            if (b != null) { branchId = b.getId(); branchName = b.getName(); }
        } else if (ds.getBranch() != null) {
            branchId   = ds.getBranch().getId();
            branchName = ds.getBranch().getName();
        }

        return new DoctorScheduleResponse(
                ds.getId(), ds.getDoctor().getId(),
                doctorBranchId, branchId, branchName,
                ds.getDayOfWeek(), ds.getStartTime(), ds.getEndTime(),
                ds.getIsActive(), ds.getSlotDurationMinutes(),
                ds.getMaxPatientsPerSlot(), ds.getValidFrom(), ds.getValidUntil());
    }

    private DoctorScheduleResponse toResponseWithDoctorBranch(DoctorSchedule ds, DoctorBranch doctorBranch) {
        Branch branch = doctorBranch.getBranch();
        return new DoctorScheduleResponse(
                ds.getId(), ds.getDoctor().getId(),
                doctorBranch.getId(),
                branch != null ? branch.getId() : null,
                branch != null ? branch.getName() : null,
                ds.getDayOfWeek(), ds.getStartTime(), ds.getEndTime(),
                ds.getIsActive(), ds.getSlotDurationMinutes(),
                ds.getMaxPatientsPerSlot(), ds.getValidFrom(), ds.getValidUntil());
    }
}
