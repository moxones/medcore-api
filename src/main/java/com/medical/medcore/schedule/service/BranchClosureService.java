package com.medical.medcore.schedule.service;

import com.medical.medcore.appointment.domain.Appointment;
import com.medical.medcore.appointment.domain.AppointmentStatusCode;
import com.medical.medcore.appointment.repository.AppointmentRepository;
import com.medical.medcore.appointment.service.AppointmentStatuses;
import com.medical.medcore.appointment.service.BookingPolicy;
import com.medical.medcore.branch.domain.Branch;
import com.medical.medcore.branch.repository.BranchRepository;
import com.medical.medcore.schedule.domain.AffectedResolution;
import com.medical.medcore.schedule.domain.BranchClosure;
import com.medical.medcore.schedule.domain.ClosureSource;
import com.medical.medcore.schedule.dto.BranchClosureRequest;
import com.medical.medcore.schedule.dto.BranchClosureResponse;
import com.medical.medcore.schedule.dto.ClosureOutcomeResponse;
import com.medical.medcore.schedule.dto.HolidayImportResponse;
import com.medical.medcore.schedule.dto.ImpactPreviewResponse;
import com.medical.medcore.schedule.repository.BranchClosureRepository;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.shared.security.BranchScope;
import com.medical.medcore.shared.security.SecurityUtils;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchClosureService {

    private static final int MAX_RANGE_DAYS = 400;

    private final BranchClosureRepository closureRepository;
    private final BranchRepository branchRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentStatuses statuses;
    private final AffectedAppointments affectedAppointments;
    private final BookingPolicy bookingPolicy;

    public List<BranchClosureResponse> list(LocalDate from, LocalDate to, Long branchId) {
        LocalDate start = from != null ? from : bookingPolicy.today();
        LocalDate end = to != null ? to : start.plusDays(365);
        if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) > MAX_RANGE_DAYS) {
            throw new BadRequestException("El rango de fechas es inválido");
        }
        if (branchId != null) {
            BranchScope.assertAccess(branchId);
        }
        List<BranchClosure> closures = closureRepository.findInRange(TenantContext.requireTenantId(), branchId,
                start, end);
        if (SecurityUtils.isBranchScoped()) {
            List<Long> allowed = TenantContext.getBranchIds() != null ? TenantContext.getBranchIds() : List.of();
            closures = closures.stream()
                    .filter(closure -> closure.getBranch() == null || allowed.contains(closure.branchId()))
                    .toList();
        }
        return toResponses(closures);
    }

    public ImpactPreviewResponse preview(BranchClosureRequest request) {
        validate(request);
        List<Appointment> affected = affectedAppointments.closedBy(request.branchId(), request.date());
        List<String> warnings = new ArrayList<>();
        if (closureRepository.existsSameScope(TenantContext.requireTenantId(), request.branchId(), request.date())) {
            warnings.add("Ya existe un cierre para esa fecha.");
        }
        return new ImpactPreviewResponse(affected.size(), affectedAppointments.toResponses(affected), false, true,
                warnings);
    }

    @Transactional
    public ClosureOutcomeResponse create(BranchClosureRequest request) {
        Branch branch = validate(request);
        Long tenantId = TenantContext.requireTenantId();
        if (closureRepository.existsSameScope(tenantId, request.branchId(), request.date())) {
            throw new ConflictException("CLOSURE_EXISTS", "Ya existe un cierre registrado para esa fecha");
        }
        BranchClosure closure = closureRepository.saveAndFlush(BranchClosure.builder()
                .tenantId(tenantId)
                .branch(branch)
                .closureDate(request.date())
                .name(request.name().trim())
                .source(ClosureSource.MANUAL)
                .build());
        AffectedAppointments.Outcome outcome = affectedAppointments.resolveForClosure(closure,
                affectedAppointments.closedBy(request.branchId(), request.date()),
                request.resolution() != null ? request.resolution() : AffectedResolution.FLAG,
                request.cancelMessage());
        return new ClosureOutcomeResponse(toResponses(List.of(closure)).get(0), outcome.flagged(),
                outcome.cancelled(), 0);
    }

    @Transactional
    public ClosureOutcomeResponse delete(Long id) {
        BranchClosure closure = closureRepository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException("Cierre no encontrado"));
        if (closure.getClosureDate().isBefore(bookingPolicy.today())) {
            throw new ConflictException("CLOSURE_PAST", "El cierre ya pasó y no se puede eliminar");
        }
        BranchClosureResponse response = toResponses(List.of(closure)).get(0);
        int restored = affectedAppointments.restoreClosure(closure.getId());
        closureRepository.delete(closure);
        return new ClosureOutcomeResponse(response, 0, 0, restored);
    }

    @Transactional
    public HolidayImportResponse importNationalHolidays(int year) {
        Long tenantId = TenantContext.requireTenantId();
        LocalDate today = bookingPolicy.today();
        int created = 0;
        int skipped = 0;
        int flagged = 0;
        List<BranchClosure> saved = new ArrayList<>();
        for (PeruHolidays.Holiday holiday : PeruHolidays.forYear(year)) {
            if (holiday.date().isBefore(today) || closureRepository.existsSameScope(tenantId, null, holiday.date())) {
                skipped++;
                continue;
            }
            BranchClosure closure = closureRepository.saveAndFlush(BranchClosure.builder()
                    .tenantId(tenantId)
                    .closureDate(holiday.date())
                    .name(holiday.name())
                    .source(ClosureSource.NATIONAL_HOLIDAY)
                    .build());
            flagged += affectedAppointments.resolveForClosure(closure,
                    affectedAppointments.closedBy(null, holiday.date()), AffectedResolution.FLAG, null).flagged();
            saved.add(closure);
            created++;
        }
        return new HolidayImportResponse(created, skipped, flagged, toResponses(saved));
    }

    private Branch validate(BranchClosureRequest request) {
        if (request.date().isBefore(bookingPolicy.today())) {
            throw new BadRequestException("PAST_DATES", "No se pueden registrar cierres en fechas pasadas");
        }
        if (request.branchId() == null) {
            return null;
        }
        return branchRepository.findByIdAndTenantId(request.branchId(), TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException("Sucursal no encontrada"));
    }

    private List<BranchClosureResponse> toResponses(List<BranchClosure> closures) {
        if (closures.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> pending = new HashMap<>();
        for (Object[] row : appointmentRepository.countPendingByClosures(TenantContext.requireTenantId(),
                statuses.idOf(AppointmentStatusCode.SCHEDULED),
                closures.stream().map(BranchClosure::getId).toList())) {
            pending.put((Long) row[0], row[1] != null ? ((Number) row[1]).longValue() : 0L);
        }
        return closures.stream()
                .map(closure -> new BranchClosureResponse(
                        closure.getId(),
                        closure.branchId(),
                        closure.getBranch() != null ? closure.getBranch().getName() : null,
                        closure.getClosureDate(),
                        closure.getName(),
                        closure.getSource().name(),
                        pending.getOrDefault(closure.getId(), 0L)))
                .toList();
    }
}
