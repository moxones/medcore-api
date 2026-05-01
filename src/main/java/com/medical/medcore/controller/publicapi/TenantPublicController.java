package com.medical.medcore.controller.publicapi;

import com.medical.medcore.dto.response.TenantInfoResponse;
import com.medical.medcore.service.tenant.TenantService;
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
    public TenantInfoResponse tenantInfo() {
        return tenantService.getTenantInfo();
    }
}