package com.medical.medcore.controller;

import com.medical.medcore.dto.request.BulkSpecialtiesRequest;
import com.medical.medcore.dto.response.CatalogItemResponse;
import com.medical.medcore.security.authorization.annotation.RequireAdminOrSuperAdmin;
import com.medical.medcore.service.doctor.DoctorSpecialtyService;
import com.medical.medcore.types.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/doctors/{doctorId}/specialties")
@RequiredArgsConstructor
@RequireAdminOrSuperAdmin
public class DoctorSpecialtyController {

    private final DoctorSpecialtyService doctorSpecialtyService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CatalogItemResponse>>> list(@PathVariable Long doctorId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                doctorSpecialtyService.listForDoctor(doctorId), "Especialidades del doctor"));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<CatalogItemResponse>>> available(@PathVariable Long doctorId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                doctorSpecialtyService.listAvailableForDoctor(doctorId), "Especialidades disponibles para asignar"));
    }

    @PostMapping("/{specialtyId}")
    public ResponseEntity<ApiResponse<CatalogItemResponse>> assign(
            @PathVariable Long doctorId, @PathVariable Long specialtyId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                doctorSpecialtyService.assign(doctorId, specialtyId), "Especialidad asignada al doctor"));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<CatalogItemResponse>>> bulkAssign(
            @PathVariable Long doctorId, @Valid @RequestBody BulkSpecialtiesRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                doctorSpecialtyService.bulkAssign(doctorId, request.specialtyIds()), "Especialidades asignadas al doctor"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<List<CatalogItemResponse>>> replaceAll(
            @PathVariable Long doctorId, @Valid @RequestBody BulkSpecialtiesRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                doctorSpecialtyService.replaceAll(doctorId, request.specialtyIds()), "Especialidades actualizadas"));
    }

    @DeleteMapping("/{specialtyId}")
    public ResponseEntity<ApiResponse<Void>> remove(
            @PathVariable Long doctorId, @PathVariable Long specialtyId) {
        doctorSpecialtyService.remove(doctorId, specialtyId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Especialidad removida del doctor"));
    }
}
