package com.medical.medcore.platform.controller;

import com.medical.medcore.platform.dto.UpdateProcessConfigRequest;
import com.medical.medcore.platform.domain.ClinicProcess;
import com.medical.medcore.shared.security.annotation.RequireAdmin;
import com.medical.medcore.shared.security.annotation.RequireStaff;
import com.medical.medcore.platform.service.TenantProcessConfigService;
import com.medical.medcore.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/process-config")
@RequiredArgsConstructor
public class TenantProcessConfigController {

    private final TenantProcessConfigService processConfigService;

    @GetMapping
    @RequireStaff
    public ResponseEntity<ApiResponse<Map<ClinicProcess, Boolean>>> getConfig() {
        return ResponseEntity.ok(
                new ApiResponse<>(true, processConfigService.getConfig(), "Configuración de procesos")
        );
    }

    @PutMapping
    @RequireAdmin
    public ResponseEntity<ApiResponse<Map<ClinicProcess, Boolean>>> updateConfig(
            @Valid @RequestBody UpdateProcessConfigRequest request) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, processConfigService.updateConfig(request.getProcesses()),
                        "Configuración de procesos actualizada")
        );
    }
}
