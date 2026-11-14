package com.medical.medcore.catalog.controller;

import com.medical.medcore.catalog.dto.CatalogItemResponse;
import com.medical.medcore.catalog.dto.PlanResponse;
import com.medical.medcore.catalog.dto.SystemCatalogResponse;
import com.medical.medcore.catalog.service.SystemCatalogService;
import com.medical.medcore.catalog.dto.TenantAppointmentTypeRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.medical.medcore.catalog.domain.AppointmentStatus;
import com.medical.medcore.platform.domain.Plan;
import com.medical.medcore.platform.domain.SubscriptionStatus;

import com.medical.medcore.shared.security.annotation.RequireAdmin;
import com.medical.medcore.catalog.service.CatalogService;
import com.medical.medcore.shared.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalogs")
@RequiredArgsConstructor

public class CatalogController {

    private final CatalogService catalogService;
    private final SystemCatalogService systemCatalogService;

    @GetMapping("/specialties")
    public ResponseEntity<ApiResponse<List<CatalogItemResponse>>> activatedSpecialties() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.listActivatedSpecialties(), "Especialidades de la clínica"));
    }

    @GetMapping("/specialties/available")
    public ResponseEntity<ApiResponse<List<CatalogItemResponse>>> availableSpecialties() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.listAvailableSpecialties(), "Especialidades disponibles"));
    }

    @RequireAdmin
    @PostMapping("/specialties/{specialtyId}")
    public ResponseEntity<ApiResponse<CatalogItemResponse>> activateSpecialty(@PathVariable Long specialtyId) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.activateSpecialty(specialtyId), "Especialidad activada"));
    }

    @RequireAdmin
    @DeleteMapping("/specialties/{specialtyId}")
    public ResponseEntity<ApiResponse<Void>> deactivateSpecialty(@PathVariable Long specialtyId) {
        catalogService.deactivateSpecialty(specialtyId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Especialidad desactivada"));
    }

    @GetMapping("/appointment-types")
    public ResponseEntity<ApiResponse<List<CatalogItemResponse>>> activatedAppointmentTypes() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.listActivatedAppointmentTypes(), "Tipos de cita de la clínica"));
    }

    @GetMapping("/appointment-types/available")
    public ResponseEntity<ApiResponse<List<CatalogItemResponse>>> availableAppointmentTypes() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.listAvailableAppointmentTypes(), "Tipos de cita disponibles"));
    }

    @RequireAdmin
    @PostMapping("/appointment-types/{appointmentTypeId}")
    public ResponseEntity<ApiResponse<CatalogItemResponse>> activateAppointmentType(
            @PathVariable Long appointmentTypeId,
            @RequestParam(required = false) Integer durationMinutes,
            @Valid @RequestBody(required = false) TenantAppointmentTypeRequest request) {
        Integer duration = request != null && request.durationMinutes() != null ? request.durationMinutes() : durationMinutes;
        return ResponseEntity.ok(new ApiResponse<>(true,
                catalogService.activateAppointmentType(appointmentTypeId, duration, request != null ? request.price() : null),
                "Tipo de cita activado"));
    }

    @RequireAdmin
    @PutMapping("/appointment-types/{appointmentTypeId}")
    public ResponseEntity<ApiResponse<CatalogItemResponse>> updateAppointmentType(
            @PathVariable Long appointmentTypeId,
            @Valid @RequestBody TenantAppointmentTypeRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                catalogService.updateAppointmentType(appointmentTypeId, request.durationMinutes(), request.price()),
                "Tipo de cita actualizado"));
    }

    @RequireAdmin
    @DeleteMapping("/appointment-types/{appointmentTypeId}")
    public ResponseEntity<ApiResponse<Void>> deactivateAppointmentType(@PathVariable Long appointmentTypeId) {
        catalogService.deactivateAppointmentType(appointmentTypeId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Tipo de cita desactivado"));
    }

    @GetMapping("/document-types")
    public ResponseEntity<ApiResponse<List<CatalogItemResponse>>> activatedDocumentTypes() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.listActivatedDocumentTypes(), "Tipos de documento de la clínica"));
    }

    @GetMapping("/document-types/available")
    public ResponseEntity<ApiResponse<List<CatalogItemResponse>>> availableDocumentTypes() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.listAvailableDocumentTypes(), "Tipos de documento disponibles"));
    }

    @RequireAdmin
    @PostMapping("/document-types/{documentTypeId}")
    public ResponseEntity<ApiResponse<CatalogItemResponse>> activateDocumentType(@PathVariable Long documentTypeId) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.activateDocumentType(documentTypeId), "Tipo de documento activado"));
    }

    @RequireAdmin
    @DeleteMapping("/document-types/{documentTypeId}")
    public ResponseEntity<ApiResponse<Void>> deactivateDocumentType(@PathVariable Long documentTypeId) {
        catalogService.deactivateDocumentType(documentTypeId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Tipo de documento desactivado"));
    }

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<PlanResponse>>> getPlans() {
        return ResponseEntity.ok(new ApiResponse<>(true, systemCatalogService.listPlans(false), "Lista de planes"));
    }

    @GetMapping("/appointment-statuses")
    public ResponseEntity<ApiResponse<List<SystemCatalogResponse>>> getAppointmentStatuses() {
        return ResponseEntity.ok(new ApiResponse<>(true,
                systemCatalogService.listAppointmentStatuses(), "Lista de estados de cita"));
    }

    @GetMapping("/subscription-statuses")
    public ResponseEntity<ApiResponse<List<SystemCatalogResponse>>> getSubscriptionStatuses() {
        return ResponseEntity.ok(new ApiResponse<>(true,
                systemCatalogService.listSubscriptionStatuses(), "Lista de estados de suscripción"));
    }
}
