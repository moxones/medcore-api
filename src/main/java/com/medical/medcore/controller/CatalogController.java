package com.medical.medcore.controller;

import com.medical.medcore.dto.response.CatalogItemResponse;
import com.medical.medcore.entity.AppointmentStatus;
import com.medical.medcore.entity.Plan;
import com.medical.medcore.entity.SubscriptionStatus;
import com.medical.medcore.security.authorization.annotation.RequireAdminOrSuperAdmin;
import com.medical.medcore.service.catalog.CatalogService;
import com.medical.medcore.types.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Catálogos desde la óptica de la CLÍNICA (tenant).
 *
 * <ul>
 *   <li>GET  /catalogs/{x}            -> ítems activados por la clínica</li>
 *   <li>GET  /catalogs/{x}/available  -> combo: maestro disponible aún no activado</li>
 *   <li>POST /catalogs/{x}/{id}       -> activar (sale del combo)</li>
 *   <li>DEL  /catalogs/{x}/{id}       -> desactivar (vuelve al combo)</li>
 * </ul>
 */
@RestController
@RequestMapping("/catalogs")
@RequiredArgsConstructor
@RequireAdminOrSuperAdmin
public class CatalogController {

    private final CatalogService catalogService;

    // ----- SPECIALTIES -----
    @GetMapping("/specialties")
    public ResponseEntity<ApiResponse<List<CatalogItemResponse>>> activatedSpecialties() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.listActivatedSpecialties(), "Especialidades de la clínica"));
    }

    @GetMapping("/specialties/available")
    public ResponseEntity<ApiResponse<List<CatalogItemResponse>>> availableSpecialties() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.listAvailableSpecialties(), "Especialidades disponibles"));
    }

    @PostMapping("/specialties/{specialtyId}")
    public ResponseEntity<ApiResponse<CatalogItemResponse>> activateSpecialty(@PathVariable Long specialtyId) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.activateSpecialty(specialtyId), "Especialidad activada"));
    }

    @DeleteMapping("/specialties/{specialtyId}")
    public ResponseEntity<ApiResponse<Void>> deactivateSpecialty(@PathVariable Long specialtyId) {
        catalogService.deactivateSpecialty(specialtyId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Especialidad desactivada"));
    }

    // ----- APPOINTMENT TYPES -----
    @GetMapping("/appointment-types")
    public ResponseEntity<ApiResponse<List<CatalogItemResponse>>> activatedAppointmentTypes() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.listActivatedAppointmentTypes(), "Tipos de cita de la clínica"));
    }

    @GetMapping("/appointment-types/available")
    public ResponseEntity<ApiResponse<List<CatalogItemResponse>>> availableAppointmentTypes() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.listAvailableAppointmentTypes(), "Tipos de cita disponibles"));
    }

    @PostMapping("/appointment-types/{appointmentTypeId}")
    public ResponseEntity<ApiResponse<CatalogItemResponse>> activateAppointmentType(
            @PathVariable Long appointmentTypeId,
            @RequestParam(required = false) Integer durationMinutes) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                catalogService.activateAppointmentType(appointmentTypeId, durationMinutes), "Tipo de cita activado"));
    }

    @DeleteMapping("/appointment-types/{appointmentTypeId}")
    public ResponseEntity<ApiResponse<Void>> deactivateAppointmentType(@PathVariable Long appointmentTypeId) {
        catalogService.deactivateAppointmentType(appointmentTypeId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Tipo de cita desactivado"));
    }

    // ----- DOCUMENT TYPES -----
    @GetMapping("/document-types")
    public ResponseEntity<ApiResponse<List<CatalogItemResponse>>> activatedDocumentTypes() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.listActivatedDocumentTypes(), "Tipos de documento de la clínica"));
    }

    @GetMapping("/document-types/available")
    public ResponseEntity<ApiResponse<List<CatalogItemResponse>>> availableDocumentTypes() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.listAvailableDocumentTypes(), "Tipos de documento disponibles"));
    }

    @PostMapping("/document-types/{documentTypeId}")
    public ResponseEntity<ApiResponse<CatalogItemResponse>> activateDocumentType(@PathVariable Long documentTypeId) {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.activateDocumentType(documentTypeId), "Tipo de documento activado"));
    }

    @DeleteMapping("/document-types/{documentTypeId}")
    public ResponseEntity<ApiResponse<Void>> deactivateDocumentType(@PathVariable Long documentTypeId) {
        catalogService.deactivateDocumentType(documentTypeId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Tipo de documento desactivado"));
    }

    // ----- SISTEMA (solo lectura para dropdowns) -----
    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<Plan>>> getPlans() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.getPlans(), "Lista de planes"));
    }

    @GetMapping("/appointment-statuses")
    public ResponseEntity<ApiResponse<List<AppointmentStatus>>> getAppointmentStatuses() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.getAppointmentStatuses(), "Lista de estados de cita"));
    }

    @GetMapping("/subscription-statuses")
    public ResponseEntity<ApiResponse<List<SubscriptionStatus>>> getSubscriptionStatuses() {
        return ResponseEntity.ok(new ApiResponse<>(true, catalogService.getSubscriptionStatuses(), "Lista de estados de suscripción"));
    }
}
