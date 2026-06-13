package com.medical.medcore.controller;

import com.medical.medcore.dto.request.UpdateProcessConfigRequest;
import com.medical.medcore.entity.enums.ClinicProcess;
import com.medical.medcore.security.authorization.annotation.RequireAdmin;
import com.medical.medcore.security.authorization.annotation.RequireStaff;
import com.medical.medcore.service.tenant.TenantProcessConfigService;
import com.medical.medcore.types.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Configuración por clínica de los procesos/etapas de la cola (triaje, cobro,
 * llamado). La lectura la consume el frontend para mostrar/ocultar pantallas;
 * la edición es solo para el administrador de la clínica.
 */
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
