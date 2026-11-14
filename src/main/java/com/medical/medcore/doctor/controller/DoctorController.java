package com.medical.medcore.doctor.controller;

import com.medical.medcore.appointment.dto.AppointmentResponse;
import com.medical.medcore.appointment.service.AppointmentService;
import com.medical.medcore.doctor.dto.DoctorCardResponse;
import com.medical.medcore.doctor.dto.DoctorRequest;
import com.medical.medcore.doctor.dto.DoctorResponse;
import com.medical.medcore.doctor.dto.DoctorSelfResponse;
import com.medical.medcore.doctor.service.DoctorListService;
import com.medical.medcore.doctor.service.DoctorService;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.api.PageableResponse;
import com.medical.medcore.shared.security.annotation.RequireAdminOrSuperAdmin;
import com.medical.medcore.shared.security.annotation.RequireDoctor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private static final String CODE_PENDING_APPOINTMENTS = "PENDING_APPOINTMENTS";

    private final DoctorService doctorService;
    private final DoctorListService doctorListService;
    private final AppointmentService appointmentService;

    @GetMapping
    public ApiResponse<PageableResponse<DoctorCardResponse>> findAll(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long specialtyId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean availableToday,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(
                doctorListService.findCards(branchId, specialtyId, isActive, availableToday, page, size), "Médicos");
    }

    @RequireDoctor
    @GetMapping("/me")
    public ApiResponse<DoctorSelfResponse> findMe() {
        return ApiResponse.ok(doctorService.getMyProfile(), "Mi perfil de médico");
    }

    @GetMapping("/{id}")
    public ApiResponse<DoctorResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(doctorService.findById(id), "Médico");
    }

    @RequireAdminOrSuperAdmin
    @PostMapping
    public ApiResponse<DoctorResponse> create(@Valid @RequestBody DoctorRequest request) {
        return ApiResponse.ok(doctorService.create(request), "Médico creado");
    }

    @RequireAdminOrSuperAdmin
    @PutMapping("/{id}")
    public ApiResponse<DoctorResponse> update(@PathVariable Long id, @Valid @RequestBody DoctorRequest request) {
        return ApiResponse.ok(doctorService.update(id, request), "Médico actualizado");
    }

    @RequireAdminOrSuperAdmin
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> deactivate(@PathVariable Long id) {
        doctorService.findById(id);
        List<AppointmentResponse> pending = appointmentService.findPendingByDoctor(id);
        if (!pending.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse<>(
                    false, pending, "El médico tiene citas pendientes", CODE_PENDING_APPOINTMENTS));
        }
        doctorService.deactivate(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Médico desactivado"));
    }
}
