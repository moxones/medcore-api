package com.medical.medcore.branch.service;

import com.medical.medcore.branch.domain.Branch;
import com.medical.medcore.branch.dto.BranchRequest;
import com.medical.medcore.branch.dto.BranchResponse;
import com.medical.medcore.branch.repository.BranchRepository;
import com.medical.medcore.shared.api.PageableResponse;
import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.ConflictException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.shared.security.BranchScope;
import com.medical.medcore.shared.security.SecurityUtils;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchService {

    private static final int MAX_PAGE_SIZE = 200;
    private static final int DEFAULT_DURATION_MINUTES = 30;
    private static final String NOT_FOUND = "Sucursal no encontrada";

    private final BranchRepository branchRepository;

    public PageableResponse<BranchResponse> findAll(int page, int size, boolean includeInactive) {
        Long tenantId = TenantContext.requireTenantId();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("name").ascending());
        BranchScope scope = BranchScope.resolve(null);
        if (scope.seesNothing()) {
            return PageableResponse.from(Page.empty(pageable));
        }
        Page<Branch> branches;
        if (scope.restricted()) {
            branches = branchRepository.findByTenantIdAndIdInAndIsActiveTrue(tenantId, scope.branchIds(), pageable);
        } else if (includeInactive && SecurityUtils.isAdmin()) {
            branches = branchRepository.findByTenantId(tenantId, pageable);
        } else {
            branches = branchRepository.findByTenantIdAndIsActiveTrue(tenantId, pageable);
        }
        return PageableResponse.from(branches.map(BranchResponse::from));
    }

    public BranchResponse findById(Long id) {
        BranchScope.assertAccess(id);
        return BranchResponse.from(load(id));
    }

    @Transactional
    public BranchResponse create(BranchRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String name = request.name().trim();
        if (branchRepository.existsByTenantIdAndNameIgnoreCase(tenantId, name)) {
            throw new ConflictException("BRANCH_NAME_TAKEN", "Ya existe una sucursal con ese nombre");
        }
        Branch branch = Branch.builder().tenantId(tenantId).name(name).build();
        apply(branch, request);
        return BranchResponse.from(branchRepository.save(branch));
    }

    @Transactional
    public BranchResponse update(Long id, BranchRequest request) {
        Branch branch = load(id);
        String name = request.name().trim();
        boolean renamed = !name.equalsIgnoreCase(branch.getName());
        if (renamed && branchRepository.existsByTenantIdAndNameIgnoreCase(branch.getTenantId(), name)) {
            throw new ConflictException("BRANCH_NAME_TAKEN", "Ya existe una sucursal con ese nombre");
        }
        branch.setName(name);
        apply(branch, request);
        return BranchResponse.from(branchRepository.save(branch));
    }

    @Transactional
    public void deactivate(Long id) {
        Branch branch = load(id);
        if (branchRepository.countUpcomingAppointments(branch.getId()) > 0) {
            throw new ConflictException("BRANCH_HAS_APPOINTMENTS",
                    "La sucursal tiene citas programadas. Reprográmalas o cancélalas antes de desactivarla");
        }
        branch.setIsActive(false);
        branchRepository.save(branch);
    }

    private void apply(Branch branch, BranchRequest request) {
        if (request.openingTime() != null && request.closingTime() != null
                && !request.closingTime().isAfter(request.openingTime())) {
            throw new BadRequestException("La hora de cierre debe ser posterior a la de apertura");
        }
        branch.setAddress(request.address() != null ? request.address().trim() : "");
        branch.setRuc(blankToNull(request.ruc()));
        branch.setPhone(blankToNull(request.phone()));
        branch.setOpeningTime(request.openingTime());
        branch.setClosingTime(request.closingTime());
        branch.setAppointmentDurationMinutes(request.appointmentDurationMinutes() != null
                ? request.appointmentDurationMinutes()
                : DEFAULT_DURATION_MINUTES);
    }

    private Branch load(Long id) {
        return branchRepository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
