package com.medical.medcore.controller;

import com.medical.medcore.dto.request.AppointmentTypeRequest;
import com.medical.medcore.dto.request.DocumentTypeRequest;
import com.medical.medcore.dto.request.SpecialtyRequest;
import com.medical.medcore.entity.AppointmentStatus;
import com.medical.medcore.entity.AppointmentType;
import com.medical.medcore.entity.DocumentType;
import com.medical.medcore.entity.Plan;
import com.medical.medcore.entity.Specialty;
import com.medical.medcore.entity.SubscriptionStatus;
import com.medical.medcore.security.authorization.annotation.RequireSuperAdmin;
import com.medical.medcore.service.catalog.CatalogService;
import com.medical.medcore.types.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestión del catálogo MAESTRO global. Solo SUPER_ADMIN.
 */
@RestController
@RequestMapping("/super-admin/catalogs")
@RequiredArgsConstructor
@RequireSuperAdmin
public class SuperAdminCatalogController {

    private final CatalogService catalogService;

    // --- SPECIALTIES (maestro) ---
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

    // --- APPOINTMENT TYPES (maestro) ---
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

    // --- DOCUMENT TYPES (maestro) ---
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

    // --- PLANS (sistema) ---
    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<Plan>>> getPlans() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.getPlans(), "Lista de planes"));
    }

    @PostMapping("/plans")
    public ResponseEntity<ApiResponse<Plan>> createPlan(@RequestBody Plan plan) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.createPlan(plan), "Plan creado"));
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<ApiResponse<Plan>> updatePlan(@PathVariable Long id, @RequestBody Plan plan) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.updatePlan(id, plan), "Plan actualizado"));
    }

    @DeleteMapping("/plans/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable Long id) {
        catalogService.deletePlan(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Plan eliminado"));
    }

    // --- SUBSCRIPTION STATUS (sistema) ---
    @GetMapping("/subscription-statuses")
    public ResponseEntity<ApiResponse<List<SubscriptionStatus>>> getSubscriptionStatuses() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.getSubscriptionStatuses(), "Lista de estados de suscripción"));
    }

    @PostMapping("/subscription-statuses")
    public ResponseEntity<ApiResponse<SubscriptionStatus>> createSubscriptionStatus(@RequestBody SubscriptionStatus status) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.createSubscriptionStatus(status), "Estado de suscripción creado"));
    }

    @PutMapping("/subscription-statuses/{id}")
    public ResponseEntity<ApiResponse<SubscriptionStatus>> updateSubscriptionStatus(@PathVariable Long id, @RequestBody SubscriptionStatus status) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.updateSubscriptionStatus(id, status), "Estado de suscripción actualizado"));
    }

    @DeleteMapping("/subscription-statuses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSubscriptionStatus(@PathVariable Long id) {
        catalogService.deleteSubscriptionStatus(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Estado de suscripción eliminado"));
    }

    // --- APPOINTMENT STATUS (sistema) ---
    @GetMapping("/appointment-statuses")
    public ResponseEntity<ApiResponse<List<AppointmentStatus>>> getAppointmentStatuses() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.getAppointmentStatuses(), "Lista de estados de cita"));
    }

    @PostMapping("/appointment-statuses")
    public ResponseEntity<ApiResponse<AppointmentStatus>> createAppointmentStatus(@RequestBody AppointmentStatus status) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.createAppointmentStatus(status), "Estado de cita creado"));
    }

    @PutMapping("/appointment-statuses/{id}")
    public ResponseEntity<ApiResponse<AppointmentStatus>> updateAppointmentStatus(@PathVariable Long id, @RequestBody AppointmentStatus status) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.updateAppointmentStatus(id, status), "Estado de cita actualizado"));
    }

    @DeleteMapping("/appointment-statuses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAppointmentStatus(@PathVariable Long id) {
        catalogService.deleteAppointmentStatus(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Estado de cita eliminado"));
    }
}
