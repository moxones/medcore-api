package com.medical.medcore.service.branch;

import com.medical.medcore.entity.Branch;
import com.medical.medcore.repository.BranchRepository;
import com.medical.medcore.types.PageableResponse;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    public PageableResponse<Branch> findAll(int page, int size) {
        Long tenantId = TenantContext.getTenantId();
        return PageableResponse.from(branchRepository.findByTenantIdAndIsActiveTrue(tenantId, PageRequest.of(page, size)));
    }

    public Branch findById(Long id) {
        return branchRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new RuntimeException("Branch no encontrada"));
    }

    public Branch create(Branch branch) {
        branch.setTenantId(TenantContext.getTenantId());
        return branchRepository.save(branch);
    }
}
