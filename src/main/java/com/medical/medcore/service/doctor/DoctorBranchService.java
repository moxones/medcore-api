package com.medical.medcore.service.doctor;

import com.medical.medcore.config.exception.BadRequestException;
import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.response.DoctorBranchResponse;
import com.medical.medcore.entity.Branch;
import com.medical.medcore.entity.Doctor;
import com.medical.medcore.entity.DoctorBranch;
import com.medical.medcore.repository.BranchRepository;
import com.medical.medcore.repository.DoctorBranchRepository;
import com.medical.medcore.repository.DoctorRepository;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorBranchService {

    private final DoctorRepository doctorRepository;
    private final BranchRepository branchRepository;
    private final DoctorBranchRepository doctorBranchRepository;

    private Doctor requireDoctorInTenant(Long doctorId) {
        Long tenantId = TenantContext.requireTenantId();
        return doctorRepository.findByIdAndTenantId(doctorId, tenantId)
                .orElseThrow(() -> new NotFoundException("Doctor no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<DoctorBranchResponse> listBranchesForDoctor(Long doctorId) {
        requireDoctorInTenant(doctorId);
        return doctorBranchRepository.findActiveBranchesByDoctorId(doctorId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DoctorBranchResponse> listDoctorsByBranch(Long branchId) {
        Long tenantId = TenantContext.requireTenantId();
        branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new NotFoundException("Sucursal no encontrada"));
        return doctorBranchRepository.findActiveDoctorsByBranchId(branchId).stream()
                .map(this::toDoctorResponse)
                .toList();
    }

    @Transactional
    public DoctorBranchResponse assign(Long doctorId, Long branchId) {
        Doctor doctor = requireDoctorInTenant(doctorId);
        Long tenantId = TenantContext.requireTenantId();

        Branch branch = branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new NotFoundException("Sucursal no encontrada"));

        DoctorBranch existing = doctorBranchRepository.findByDoctor_IdAndBranch_Id(doctorId, branchId)
                .orElse(null);

        if (existing != null) {
            if (Boolean.TRUE.equals(existing.getIsActive())) {
                throw new BadRequestException("El doctor ya está asignado a esta sucursal");
            }
            existing.setIsActive(true);
            return toResponse(doctorBranchRepository.save(existing));
        }

        DoctorBranch doctorBranch = DoctorBranch.builder()
                .doctor(doctor)
                .branch(branch)
                .createdBy(TenantContext.requireCurrentUserId())
                .build();
        doctorBranch = doctorBranchRepository.save(doctorBranch);
        return toResponse(doctorBranch);
    }

    @Transactional
    public List<DoctorBranchResponse> bulkAssign(Long doctorId, List<Long> branchIds) {
        Doctor doctor = requireDoctorInTenant(doctorId);
        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.requireCurrentUserId();

        List<DoctorBranchResponse> results = new ArrayList<>();
        for (Long branchId : branchIds) {
            Branch branch = branchRepository.findByIdAndTenantId(branchId, tenantId)
                    .orElseThrow(() -> new NotFoundException("Sucursal no encontrada: " + branchId));

            DoctorBranch existing = doctorBranchRepository.findByDoctor_IdAndBranch_Id(doctorId, branchId)
                    .orElse(null);

            if (existing != null) {
                if (Boolean.TRUE.equals(existing.getIsActive())) {
                    throw new BadRequestException("El doctor ya está asignado a la sucursal: " + branchId);
                }
                existing.setIsActive(true);
                results.add(toResponse(doctorBranchRepository.save(existing)));
                continue;
            }

            DoctorBranch doctorBranch = DoctorBranch.builder()
                    .doctor(doctor)
                    .branch(branch)
                    .createdBy(userId)
                    .build();
            results.add(toResponse(doctorBranchRepository.save(doctorBranch)));
        }
        return results;
    }

    @Transactional
    public void deactivate(Long doctorId, Long branchId) {
        requireDoctorInTenant(doctorId);
        DoctorBranch doctorBranch = doctorBranchRepository.findByDoctor_IdAndBranch_Id(doctorId, branchId)
                .orElseThrow(() -> new NotFoundException("El doctor no está asignado a esta sucursal"));
        doctorBranch.setIsActive(false);
        doctorBranchRepository.save(doctorBranch);
    }

    private DoctorBranchResponse toResponse(DoctorBranch db) {
        return new DoctorBranchResponse(
                db.getId(),
                db.getDoctor().getId(),
                null,
                null,
                db.getBranch().getId(),
                db.getBranch().getName(),
                db.getBranch().getAddress(),
                db.getIsActive(),
                db.getCreatedAt()
        );
    }

    private DoctorBranchResponse toDoctorResponse(DoctorBranch db) {
        var person = db.getDoctor().getPerson();
        String fullName = person != null
                ? "Dr. " + person.getFirstName() + " " + person.getLastName()
                : null;
        return new DoctorBranchResponse(
                db.getId(),
                db.getDoctor().getId(),
                fullName,
                db.getDoctor().getLicenseNumber(),
                db.getBranch().getId(),
                db.getBranch().getName(),
                db.getBranch().getAddress(),
                db.getIsActive(),
                db.getCreatedAt()
        );
    }
}
