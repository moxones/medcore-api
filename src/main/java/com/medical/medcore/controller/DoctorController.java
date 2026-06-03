package com.medical.medcore.controller;

import com.medical.medcore.dto.response.DoctorCardResponse;
import com.medical.medcore.entity.Doctor;
import com.medical.medcore.security.authorization.annotation.RequireAdminOrSuperAdmin;
import com.medical.medcore.service.doctor.DoctorListService;
import com.medical.medcore.service.doctor.DoctorService;
import com.medical.medcore.types.ApiResponse;
import com.medical.medcore.types.PageableResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final DoctorListService doctorListService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageableResponse<DoctorCardResponse>>> findAll(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long specialtyId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean availableToday,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                doctorListService.findCards(branchId, specialtyId, isActive, availableToday, page, size),
                "Médicos"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Doctor>> findMe() {
        return ResponseEntity.ok(new ApiResponse<>(true, doctorService.findMe(), "Mi perfil de médico"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Doctor>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, doctorService.findById(id), "Médico"));
    }

    @RequireAdminOrSuperAdmin
    @PostMapping
    public ResponseEntity<ApiResponse<Doctor>> create(@RequestBody Doctor doctor) {
        return ResponseEntity.ok(new ApiResponse<>(true, doctorService.create(doctor), "Médico creado"));
    }

    @RequireAdminOrSuperAdmin
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Doctor>> update(@PathVariable Long id, @RequestBody Doctor doctor) {
        return ResponseEntity.ok(new ApiResponse<>(true, doctorService.update(id, doctor), "Médico actualizado"));
    }
}
