package com.medical.medcore.service.branch;

import com.medical.medcore.config.exception.BadRequestException;
import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.entity.Branch;
import com.medical.medcore.repository.BranchRepository;
import com.medical.medcore.security.authorization.SecurityUtils;
import com.medical.medcore.types.PageableResponse;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    public PageableResponse<Branch> findAll(int page, int size) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BadRequestException("Tenant no disponible");
        }

        // El personal operativo solo ve las sucursales que tiene asignadas
        if (SecurityUtils.isBranchScoped()) {
            List<Long> allowed = TenantContext.getBranchIds();
            if (allowed == null || allowed.isEmpty()) {
                return PageableResponse.from(Page.empty(PageRequest.of(page, size)));
            }
            return PageableResponse.from(
                    branchRepository.findByTenantIdAndIdInAndIsActiveTrue(tenantId, allowed, PageRequest.of(page, size)));
        }

        return PageableResponse.from(branchRepository.findByTenantIdAndIsActiveTrue(tenantId, PageRequest.of(page, size)));
    }

    public Branch findById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BadRequestException("Tenant no disponible");
        }

        if (SecurityUtils.isBranchScoped()) {
            List<Long> allowed = TenantContext.getBranchIds();
            if (allowed == null || !allowed.contains(id)) {
                throw new AccessDeniedException("No tienes acceso a esta sucursal");
            }
        }

        return branchRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Sucursal no encontrada"));
    }

    public Branch create(Branch branch) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BadRequestException("Tenant no disponible");
        }
        branch.setTenantId(tenantId);
        return branchRepository.save(branch);
    }
}
