package com.medical.medcore.service.tenant;

import com.medical.medcore.dto.request.CreateTenantRequest;
import com.medical.medcore.dto.request.UpdateTenantRequest;
import com.medical.medcore.dto.response.TenantInfoResponse;
import com.medical.medcore.dto.response.TenantResponse;

import java.util.List;

public interface TenantService {
    TenantInfoResponse getTenantInfo();
    
    TenantResponse createTenant(CreateTenantRequest request);
    
    TenantResponse updateTenant(Long id, UpdateTenantRequest request);
    
    TenantResponse getTenantById(Long id);
    
    List<TenantResponse> getAllTenants();
    
    void deleteTenant(Long id);
}
