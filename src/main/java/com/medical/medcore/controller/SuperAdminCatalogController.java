package com.medical.medcore.controller;

import com.medical.medcore.entity.AppointmentType;
import com.medical.medcore.entity.Specialty;
import com.medical.medcore.security.authorization.annotation.RequireSuperAdmin;
import com.medical.medcore.service.catalog.CatalogService;
import com.medical.medcore.types.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/super-admin/catalogs")
@RequireSuperAdmin
public class SuperAdminCatalogController {

    private final CatalogService catalogService;

    public SuperAdminCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/specialties")
    public ResponseEntity<ApiResponse<List<Specialty>>> getSpecialties(
            @RequestParam(required = false) Long tenantId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, catalogService.getSpecialtiesForSuperAdmin(tenantId), "Lista de especialidades")
        );
    }

    @GetMapping("/appointment-types")
    public ResponseEntity<ApiResponse<List<AppointmentType>>> getAppointmentTypes(
            @RequestParam(required = false) Long tenantId) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, catalogService.getAppointmentTypesForSuperAdmin(tenantId), "Lista de tipos de cita")
        );
    }

    @PostMapping("/specialties")
    public ResponseEntity<ApiResponse<Specialty>> createSpecialty(
            @RequestParam Long tenantId,
            @RequestBody Specialty specialty) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, catalogService.createSpecialtyForTenant(tenantId, specialty), "Especialidad creada")
        );
    }

    @PostMapping("/appointment-types")
    public ResponseEntity<ApiResponse<AppointmentType>> createAppointmentType(
            @RequestParam Long tenantId,
            @RequestBody AppointmentType type) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, catalogService.createAppointmentTypeForTenant(tenantId, type), "Tipo de cita creado")
        );
    }

    @PutMapping("/specialties/{id}")
    public ResponseEntity<ApiResponse<Specialty>> updateSpecialty(
            @PathVariable Long id,
            @RequestBody Specialty specialty) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, catalogService.updateSpecialtyForSuperAdmin(id, specialty), "Especialidad actualizada")
        );
    }

    @PutMapping("/appointment-types/{id}")
    public ResponseEntity<ApiResponse<AppointmentType>> updateAppointmentType(
            @PathVariable Long id,
            @RequestBody AppointmentType type) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, catalogService.updateAppointmentTypeForSuperAdmin(id, type), "Tipo de cita actualizado")
        );
    }

    @DeleteMapping("/specialties/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSpecialty(@PathVariable Long id) {
        catalogService.deleteSpecialtyForSuperAdmin(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Especialidad eliminada"));
    }

    @DeleteMapping("/appointment-types/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAppointmentType(@PathVariable Long id) {
        catalogService.deleteAppointmentTypeForSuperAdmin(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Tipo de cita eliminado"));
    }
}