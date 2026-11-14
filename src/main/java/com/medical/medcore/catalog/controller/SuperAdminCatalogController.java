package com.medical.medcore.catalog.controller;

import com.medical.medcore.catalog.dto.PlanRequest;
import com.medical.medcore.catalog.dto.PlanResponse;
import com.medical.medcore.catalog.dto.SystemCatalogRequest;
import com.medical.medcore.catalog.dto.SystemCatalogResponse;
import com.medical.medcore.catalog.service.SystemCatalogService;
import jakarta.validation.Valid;

import com.medical.medcore.catalog.dto.AppointmentTypeRequest;
import com.medical.medcore.catalog.dto.DocumentTypeRequest;
import com.medical.medcore.catalog.dto.SpecialtyRequest;
import com.medical.medcore.catalog.domain.AppointmentStatus;
import com.medical.medcore.catalog.domain.AppointmentType;
import com.medical.medcore.catalog.domain.DocumentType;
import com.medical.medcore.platform.domain.Plan;
import com.medical.medcore.catalog.domain.Specialty;
import com.medical.medcore.platform.domain.SubscriptionStatus;
import com.medical.medcore.shared.security.annotation.RequireSuperAdmin;
import com.medical.medcore.catalog.service.CatalogService;
import com.medical.medcore.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/super-admin/catalogs")
@RequiredArgsConstructor
@RequireSuperAdmin
public class SuperAdminCatalogController {

    private final CatalogService catalogService;
    private final SystemCatalogService systemCatalogService;

    @GetMapping("/specialties")
    public ResponseEntity<ApiResponse<List<Specialty>>> getSpecialties() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.listMasterSpecialties(), "Catálogo maestro de especialidades"));
    }

    @PostMapping("/specialties")
    public ResponseEntity<ApiResponse<Specialty>> createSpecialty(@Valid @RequestBody SpecialtyRequest req) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.createSpecialty(req), "Especialidad creada"));
    }

    @PutMapping("/specialties/{id}")
    public ResponseEntity<ApiResponse<Specialty>> updateSpecialty(@PathVariable Long id, @Valid @RequestBody SpecialtyRequest req) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.updateSpecialty(id, req), "Especialidad actualizada"));
    }

    @DeleteMapping("/specialties/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSpecialty(@PathVariable Long id) {
        catalogService.deleteSpecialty(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Especialidad eliminada"));
    }

    @GetMapping("/appointment-types")
    public ResponseEntity<ApiResponse<List<AppointmentType>>> getAppointmentTypes() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.listMasterAppointmentTypes(), "Catálogo maestro de tipos de cita"));
    }

    @PostMapping("/appointment-types")
    public ResponseEntity<ApiResponse<AppointmentType>> createAppointmentType(@Valid @RequestBody AppointmentTypeRequest req) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.createAppointmentType(req), "Tipo de cita creado"));
    }

    @PutMapping("/appointment-types/{id}")
    public ResponseEntity<ApiResponse<AppointmentType>> updateAppointmentType(@PathVariable Long id, @Valid @RequestBody AppointmentTypeRequest req) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.updateAppointmentType(id, req), "Tipo de cita actualizado"));
    }

    @DeleteMapping("/appointment-types/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAppointmentType(@PathVariable Long id) {
        catalogService.deleteAppointmentType(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Tipo de cita eliminado"));
    }

    @GetMapping("/document-types")
    public ResponseEntity<ApiResponse<List<DocumentType>>> getDocumentTypes() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.listMasterDocumentTypes(), "Catálogo maestro de tipos de documento"));
    }

    @PostMapping("/document-types")
    public ResponseEntity<ApiResponse<DocumentType>> createDocumentType(@Valid @RequestBody DocumentTypeRequest req) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.createDocumentType(req), "Tipo de documento creado"));
    }

    @PutMapping("/document-types/{id}")
    public ResponseEntity<ApiResponse<DocumentType>> updateDocumentType(@PathVariable Long id, @Valid @RequestBody DocumentTypeRequest req) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.updateDocumentType(id, req), "Tipo de documento actualizado"));
    }

    @DeleteMapping("/document-types/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocumentType(@PathVariable Long id) {
        catalogService.deleteDocumentType(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Tipo de documento eliminado"));
    }

    @GetMapping("/plans")
    public ApiResponse<List<PlanResponse>> getPlans() {
        return ApiResponse.ok(systemCatalogService.listPlans(true), "Lista de planes");
    }

    @PostMapping("/plans")
    public ApiResponse<PlanResponse> createPlan(@Valid @RequestBody PlanRequest request) {
        return ApiResponse.ok(systemCatalogService.createPlan(request), "Plan creado");
    }

    @PutMapping("/plans/{id}")
    public ApiResponse<PlanResponse> updatePlan(@PathVariable Long id, @Valid @RequestBody PlanRequest request) {
        return ApiResponse.ok(systemCatalogService.updatePlan(id, request), "Plan actualizado");
    }

    @DeleteMapping("/plans/{id}")
    public ApiResponse<Void> deactivatePlan(@PathVariable Long id) {
        systemCatalogService.deactivatePlan(id);
        return ApiResponse.message("Plan desactivado");
    }

    @GetMapping("/subscription-statuses")
    public ApiResponse<List<SystemCatalogResponse>> getSubscriptionStatuses() {
        return ApiResponse.ok(systemCatalogService.listSubscriptionStatuses(), "Lista de estados de suscripción");
    }

    @PostMapping("/subscription-statuses")
    public ApiResponse<SystemCatalogResponse> createSubscriptionStatus(@Valid @RequestBody SystemCatalogRequest request) {
        return ApiResponse.ok(systemCatalogService.createSubscriptionStatus(request), "Estado de suscripción creado");
    }

    @PutMapping("/subscription-statuses/{id}")
    public ApiResponse<SystemCatalogResponse> updateSubscriptionStatus(
            @PathVariable Long id, @Valid @RequestBody SystemCatalogRequest request) {
        return ApiResponse.ok(systemCatalogService.updateSubscriptionStatus(id, request), "Estado de suscripción actualizado");
    }

    @DeleteMapping("/subscription-statuses/{id}")
    public ApiResponse<Void> deleteSubscriptionStatus(@PathVariable Long id) {
        systemCatalogService.deleteSubscriptionStatus(id);
        return ApiResponse.message("Estado de suscripción eliminado");
    }

    @GetMapping("/appointment-statuses")
    public ApiResponse<List<SystemCatalogResponse>> getAppointmentStatuses() {
        return ApiResponse.ok(systemCatalogService.listAppointmentStatuses(), "Lista de estados de cita");
    }

    @PostMapping("/appointment-statuses")
    public ApiResponse<SystemCatalogResponse> createAppointmentStatus(@Valid @RequestBody SystemCatalogRequest request) {
        return ApiResponse.ok(systemCatalogService.createAppointmentStatus(request), "Estado de cita creado");
    }

    @PutMapping("/appointment-statuses/{id}")
    public ApiResponse<SystemCatalogResponse> updateAppointmentStatus(
            @PathVariable Long id, @Valid @RequestBody SystemCatalogRequest request) {
        return ApiResponse.ok(systemCatalogService.updateAppointmentStatus(id, request), "Estado de cita actualizado");
    }

    @DeleteMapping("/appointment-statuses/{id}")
    public ApiResponse<Void> deleteAppointmentStatus(@PathVariable Long id) {
        systemCatalogService.deleteAppointmentStatus(id);
        return ApiResponse.message("Estado de cita eliminado");
    }
}
