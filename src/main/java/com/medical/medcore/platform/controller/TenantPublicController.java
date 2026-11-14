package com.medical.medcore.platform.controller;

import com.medical.medcore.platform.dto.TenantInfoResponse;
import com.medical.medcore.platform.service.TenantService;
import com.medical.medcore.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class TenantPublicController {

    private final TenantService tenantService;

    @GetMapping("/tenant-info")
    public ApiResponse<TenantInfoResponse> tenantInfo() {
        return ApiResponse.ok(tenantService.getTenantInfo());
    }
}
