package com.medical.medcore.service.staff;

import com.medical.medcore.config.exception.BadRequestException;
import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.response.StaffBranchResponse;
import com.medical.medcore.entity.Branch;
import com.medical.medcore.entity.Person;
import com.medical.medcore.entity.StaffBranch;
import com.medical.medcore.entity.User;
import com.medical.medcore.repository.BranchRepository;
import com.medical.medcore.repository.StaffBranchRepository;
import com.medical.medcore.repository.UserRepository;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffBranchService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final StaffBranchRepository staffBranchRepository;

    private User requireUserInTenant(Long userId) {
        Long tenantId = TenantContext.requireTenantId();
        return userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<StaffBranchResponse> listBranchesForUser(Long userId) {
        User user = requireUserInTenant(userId);
        return staffBranchRepository.findActiveBranchesByUserId(userId).stream()
                .map(sb -> toResponse(sb, user))
                .toList();
    }

    @Transactional
    public StaffBranchResponse assign(Long userId, Long branchId) {
        User user = requireUserInTenant(userId);
        Long tenantId = TenantContext.requireTenantId();

        Branch branch = branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new NotFoundException("Sucursal no encontrada"));

        StaffBranch existing = staffBranchRepository.findByUser_IdAndBranch_Id(userId, branchId)
                .orElse(null);

        if (existing != null) {
            if (Boolean.TRUE.equals(existing.getIsActive())) {
                throw new BadRequestException("El usuario ya está asignado a esta sucursal");
            }
            existing.setIsActive(true);
            existing.setBranch(branch);
            return toResponse(staffBranchRepository.save(existing), user);
        }

        StaffBranch staffBranch = StaffBranch.builder()
                .user(user)
                .branch(branch)
                .createdBy(TenantContext.requireCurrentUserId())
                .build();
        return toResponse(staffBranchRepository.save(staffBranch), user);
    }

    @Transactional
    public List<StaffBranchResponse> bulkAssign(Long userId, List<Long> branchIds) {
        User user = requireUserInTenant(userId);
        Long tenantId = TenantContext.requireTenantId();
        Long createdBy = TenantContext.requireCurrentUserId();

        List<StaffBranchResponse> results = new ArrayList<>();
        for (Long branchId : branchIds) {
            Branch branch = branchRepository.findByIdAndTenantId(branchId, tenantId)
                    .orElseThrow(() -> new NotFoundException("Sucursal no encontrada: " + branchId));

            StaffBranch existing = staffBranchRepository.findByUser_IdAndBranch_Id(userId, branchId)
                    .orElse(null);

            if (existing != null) {
                if (Boolean.TRUE.equals(existing.getIsActive())) {
                    throw new BadRequestException("El usuario ya está asignado a la sucursal: " + branchId);
                }
                existing.setIsActive(true);
                existing.setBranch(branch);
                results.add(toResponse(staffBranchRepository.save(existing), user));
                continue;
            }

            StaffBranch staffBranch = StaffBranch.builder()
                    .user(user)
                    .branch(branch)
                    .createdBy(createdBy)
                    .build();
            results.add(toResponse(staffBranchRepository.save(staffBranch), user));
        }
        return results;
    }

    @Transactional
    public void deactivate(Long userId, Long branchId) {
        requireUserInTenant(userId);
        StaffBranch staffBranch = staffBranchRepository.findByUser_IdAndBranch_Id(userId, branchId)
                .orElseThrow(() -> new NotFoundException("El usuario no está asignado a esta sucursal"));
        staffBranch.setIsActive(false);
        staffBranchRepository.save(staffBranch);
    }

    private StaffBranchResponse toResponse(StaffBranch sb, User user) {
        Person person = user.getPerson();
        String fullName = person != null
                ? person.getFirstName() + " " + person.getLastName()
                : null;
        return new StaffBranchResponse(
                sb.getId(),
                user.getId(),
                fullName,
                user.getEmail(),
                sb.getBranch().getId(),
                sb.getBranch().getName(),
                sb.getBranch().getAddress(),
                sb.getIsActive(),
                sb.getCreatedAt()
        );
    }
}
