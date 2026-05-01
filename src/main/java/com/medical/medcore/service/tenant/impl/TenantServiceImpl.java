package com.medical.medcore.service.tenant.impl;

import com.medical.medcore.dto.request.CreateTenantRequest;
import com.medical.medcore.dto.request.UpdateTenantRequest;
import com.medical.medcore.dto.response.TenantInfoResponse;
import com.medical.medcore.dto.response.TenantResponse;
import com.medical.medcore.entity.Tenant;
import com.medical.medcore.entity.enums.TenantStatus;
import com.medical.medcore.repository.TenantRepository;
import com.medical.medcore.service.tenant.TenantService;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;

    @Override
    public TenantInfoResponse getTenantInfo() {
        Long tenantId = TenantContext.getTenantId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        return TenantInfoResponse.builder()
                .name(tenant.getName())
                .logoUrl(tenant.getLogoUrl())
                .subtitle(tenant.getSubtitle())
                .build();
    }

    @Override
    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {
        if (tenantRepository.existsBySubdomain(request.getSubdomain())) {
            throw new RuntimeException("Subdomain already exists");
        }

        Tenant tenant = Tenant.builder()
                .subdomain(request.getSubdomain())
                .name(request.getName())
                .logoUrl(request.getLogoUrl())
                .primaryColor(request.getPrimaryColor())
                .subtitle(request.getSubtitle())
                .status(TenantStatus.ACTIVE)
                .build();

        Tenant savedTenant = tenantRepository.save(tenant);
        return mapToResponse(savedTenant);
    }

    @Override
    @Transactional
    public TenantResponse updateTenant(Long id, UpdateTenantRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        tenant.setName(request.getName());
        tenant.setLogoUrl(request.getLogoUrl());
        tenant.setPrimaryColor(request.getPrimaryColor());
        tenant.setSubtitle(request.getSubtitle());
        tenant.setStatus(request.getStatus());

        Tenant updatedTenant = tenantRepository.save(tenant);
        return mapToResponse(updatedTenant);
    }

    @Override
    public TenantResponse getTenantById(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        return mapToResponse(tenant);
    }

    @Override
    public List<TenantResponse> getAllTenants() {
        List<Tenant> tenants = tenantRepository.findAll();
        return tenants.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteTenant(Long id) {
        if (!tenantRepository.existsById(id)) {
            throw new RuntimeException("Tenant not found");
        }
        tenantRepository.deleteById(id);
    }

    private TenantResponse mapToResponse(Tenant tenant) {
        return TenantResponse.builder()
                .id(tenant.getId())
                .subdomain(tenant.getSubdomain())
                .name(tenant.getName())
                .status(tenant.getStatus())
                .logoUrl(tenant.getLogoUrl())
                .primaryColor(tenant.getPrimaryColor())
                .subtitle(tenant.getSubtitle())
                .createdAt(tenant.getCreatedAt())
                .build();
    }
}