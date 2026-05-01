package com.medical.medcore.controller;

import com.medical.medcore.entity.*;
import com.medical.medcore.security.authorization.annotation.RequireAdminOrSuperAdmin;
import com.medical.medcore.service.catalog.CatalogService;
import com.medical.medcore.types.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalogs")
@RequiredArgsConstructor
@RequireAdminOrSuperAdmin
public class CatalogController {

    private final CatalogService catalogService;

    // --- SPECIALTIES ---
    @GetMapping("/specialties")
    public ResponseEntity<ApiResponse<List<Specialty>>> getSpecialties() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.getSpecialties(), "Lista de especialidades"));
    }
    @GetMapping("/specialties/{id}")
    public ResponseEntity<ApiResponse<Specialty>> getSpecialtyById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.getSpecialtyById(id), "Especialidad encontrada"));
    }
    @PostMapping("/specialties")
    public ResponseEntity<ApiResponse<Specialty>> createSpecialty(@RequestBody Specialty specialty) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.createSpecialty(specialty), "Especialidad creada"));
    }
    @PutMapping("/specialties/{id}")
    public ResponseEntity<ApiResponse<Specialty>> updateSpecialty(@PathVariable Long id, @RequestBody Specialty specialty) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.updateSpecialty(id, specialty), "Especialidad actualizada"));
    }
    @DeleteMapping("/specialties/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSpecialty(@PathVariable Long id) {
        catalogService.deleteSpecialty(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Especialidad eliminada"));
    }

    // --- PLANS ---
    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<Plan>>> getPlans() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.getPlans(), "Lista de planes"));
    }
    @GetMapping("/plans/{id}")
    public ResponseEntity<ApiResponse<Plan>> getPlanById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.getPlanById(id), "Plan encontrado"));
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

    // --- DOCUMENT TYPES ---
    @GetMapping("/document-types")
    public ResponseEntity<ApiResponse<List<DocumentType>>> getDocumentTypes() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.getDocumentTypes(), "Lista de tipos de documento"));
    }
    @GetMapping("/document-types/{id}")
    public ResponseEntity<ApiResponse<DocumentType>> getDocumentTypeById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.getDocumentTypeById(id), "Tipo de documento encontrado"));
    }
    @PostMapping("/document-types")
    public ResponseEntity<ApiResponse<DocumentType>> createDocumentType(@RequestBody DocumentType docType) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.createDocumentType(docType), "Tipo de documento creado"));
    }
    @PutMapping("/document-types/{id}")
    public ResponseEntity<ApiResponse<DocumentType>> updateDocumentType(@PathVariable Long id, @RequestBody DocumentType docType) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.updateDocumentType(id, docType), "Tipo de documento actualizado"));
    }
    @DeleteMapping("/document-types/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocumentType(@PathVariable Long id) {
        catalogService.deleteDocumentType(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Tipo de documento eliminado"));
    }

    // --- SUBSCRIPTION STATUS ---
    @GetMapping("/subscription-statuses")
    public ResponseEntity<ApiResponse<List<SubscriptionStatus>>> getSubscriptionStatuses() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.getSubscriptionStatuses(), "Lista de estados de suscripción"));
    }
    @GetMapping("/subscription-statuses/{id}")
    public ResponseEntity<ApiResponse<SubscriptionStatus>> getSubscriptionStatusById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.getSubscriptionStatusById(id), "Estado de suscripción encontrado"));
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

    // --- APPOINTMENT STATUS ---
    @GetMapping("/appointment-statuses")
    public ResponseEntity<ApiResponse<List<AppointmentStatus>>> getAppointmentStatuses() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.getAppointmentStatuses(), "Lista de estados de cita"));
    }
    @GetMapping("/appointment-statuses/{id}")
    public ResponseEntity<ApiResponse<AppointmentStatus>> getAppointmentStatusById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.getAppointmentStatusById(id), "Estado de cita encontrado"));
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

    // --- APPOINTMENT TYPE ---
    @GetMapping("/appointment-types")
    public ResponseEntity<ApiResponse<List<AppointmentType>>> getAppointmentTypes() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.getAppointmentTypes(), "Lista de tipos de cita"));
    }
    @GetMapping("/appointment-types/{id}")
    public ResponseEntity<ApiResponse<AppointmentType>> getAppointmentTypeById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.getAppointmentTypeById(id), "Tipo de cita encontrado"));
    }
    @PostMapping("/appointment-types")
    public ResponseEntity<ApiResponse<AppointmentType>> createAppointmentType(@RequestBody AppointmentType type) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.createAppointmentType(type), "Tipo de cita creado"));
    }
    @PutMapping("/appointment-types/{id}")
    public ResponseEntity<ApiResponse<AppointmentType>> updateAppointmentType(@PathVariable Long id, @RequestBody AppointmentType type) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.updateAppointmentType(id, type), "Tipo de cita actualizado"));
    }
    @DeleteMapping("/appointment-types/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAppointmentType(@PathVariable Long id) {
        catalogService.deleteAppointmentType(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Tipo de cita eliminada"));
    }
}
