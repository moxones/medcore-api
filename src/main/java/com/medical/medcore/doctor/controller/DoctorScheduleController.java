package com.medical.medcore.doctor.controller;

import com.medical.medcore.doctor.dto.DoctorScheduleRequest;
import com.medical.medcore.doctor.dto.DoctorScheduleUpdateRequest;
import com.medical.medcore.doctor.dto.DoctorScheduleResponse;
import com.medical.medcore.shared.security.annotation.RequireDoctorOrAdminOrSuperAdmin;
import com.medical.medcore.doctor.service.DoctorScheduleService;
import com.medical.medcore.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/doctors/{doctorId}/schedules")
@RequiredArgsConstructor
@RequireDoctorOrAdminOrSuperAdmin
public class DoctorScheduleController {

    private final DoctorScheduleService doctorScheduleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DoctorScheduleResponse>>> list(
            @PathVariable Long doctorId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Integer dayOfWeek,
            @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                doctorScheduleService.listForDoctor(doctorId, branchId, dayOfWeek, isActive),
                "Horarios del doctor"));
    }

    @GetMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<DoctorScheduleResponse>> findById(
            @PathVariable Long doctorId,
            @PathVariable Long scheduleId) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                doctorScheduleService.findById(doctorId, scheduleId),
                "Horario"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DoctorScheduleResponse>> create(
            @PathVariable Long doctorId,
            @Valid @RequestBody DoctorScheduleRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                doctorScheduleService.create(doctorId, request),
                "Horario creado"));
    }

    @PutMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<DoctorScheduleResponse>> update(
            @PathVariable Long doctorId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody DoctorScheduleUpdateRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                doctorScheduleService.update(doctorId, scheduleId, request),
                "Horario actualizado"));
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable Long doctorId,
            @PathVariable Long scheduleId) {
        doctorScheduleService.deactivate(doctorId, scheduleId);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Horario desactivado"));
    }
}
