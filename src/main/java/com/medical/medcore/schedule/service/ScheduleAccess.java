package com.medical.medcore.schedule.service;

import com.medical.medcore.doctor.domain.Doctor;
import com.medical.medcore.doctor.domain.DoctorBranch;
import com.medical.medcore.doctor.repository.DoctorBranchRepository;
import com.medical.medcore.doctor.repository.DoctorRepository;
import com.medical.medcore.identity.repository.UserRepository;
import com.medical.medcore.platform.domain.ClinicProcess;
import com.medical.medcore.platform.service.TenantProcessConfigService;
import com.medical.medcore.shared.exception.ForbiddenException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.shared.security.RoleConstants;
import com.medical.medcore.shared.security.SecurityUtils;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ScheduleAccess {

    private static final String DENIED = "No puedes gestionar la agenda de este profesional";

    private final DoctorRepository doctorRepository;
    private final DoctorBranchRepository doctorBranchRepository;
    private final UserRepository userRepository;
    private final TenantProcessConfigService processConfigService;

    public Doctor requireDoctor(Long doctorId) {
        return doctorRepository.findByIdAndTenantId(doctorId, TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException("Profesional no encontrado"));
    }

    public Doctor requireReadable(Long doctorId) {
        Doctor doctor = requireDoctor(doctorId);
        assertReadable(doctor);
        return doctor;
    }

    public Doctor requireManageable(Long doctorId) {
        Doctor doctor = requireDoctor(doctorId);
        assertManageable(doctor);
        return doctor;
    }

    public void assertReadable(Doctor doctor) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        if (SecurityUtils.isBranchScoped()) {
            assertSharesBranch(doctor);
            return;
        }
        if (SecurityUtils.isDoctor()) {
            assertOwn(doctor);
            return;
        }
        throw new ForbiddenException(DENIED);
    }

    public void assertManageable(Doctor doctor) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        if (SecurityUtils.isBranchScoped() && SecurityUtils.hasAnyRole(RoleConstants.RECEPTIONIST)) {
            assertSharesBranch(doctor);
            return;
        }
        if (SecurityUtils.isDoctor() && !SecurityUtils.isBranchScoped()) {
            assertOwn(doctor);
            return;
        }
        throw new ForbiddenException(DENIED);
    }

    public boolean actsAsOwnProfessional() {
        return SecurityUtils.isDoctorOnly() && !SecurityUtils.isBranchScoped();
    }

    public boolean requiresApproval() {
        return actsAsOwnProfessional()
                && Boolean.TRUE.equals(processConfigService.getConfig().get(ClinicProcess.ABSENCE_APPROVAL));
    }

    public Optional<Long> ownDoctorId() {
        if (!SecurityUtils.isDoctorOnly()) {
            return Optional.empty();
        }
        Long tenantId = TenantContext.requireTenantId();
        return userRepository.findByIdAndTenantId(TenantContext.requireCurrentUserId(), tenantId)
                .flatMap(user -> doctorRepository.findByPersonIdAndTenantId(user.getPerson().getId(), tenantId))
                .map(Doctor::getId);
    }

    public Set<Long> branchIdsOf(Long doctorId) {
        return doctorBranchRepository.findActiveBranchesByDoctorId(doctorId).stream()
                .map(DoctorBranch::getBranch)
                .map(branch -> branch.getId())
                .collect(Collectors.toSet());
    }

    public boolean visibleToBranchScope(Long doctorId) {
        if (!SecurityUtils.isBranchScoped()) {
            return true;
        }
        List<Long> allowed = scopeBranches();
        return branchIdsOf(doctorId).stream().anyMatch(allowed::contains);
    }

    private void assertSharesBranch(Doctor doctor) {
        if (!visibleToBranchScope(doctor.getId())) {
            throw new ForbiddenException(DENIED);
        }
    }

    private void assertOwn(Doctor doctor) {
        Long ownPersonId = userRepository.findByIdAndTenantId(TenantContext.requireCurrentUserId(),
                        TenantContext.requireTenantId())
                .map(user -> user.getPerson().getId())
                .orElse(null);
        if (!doctor.getPerson().getId().equals(ownPersonId)) {
            throw new ForbiddenException("Solo puedes gestionar tu propia agenda");
        }
    }

    private List<Long> scopeBranches() {
        List<Long> allowed = TenantContext.getBranchIds();
        return allowed != null ? allowed : List.of();
    }
}
