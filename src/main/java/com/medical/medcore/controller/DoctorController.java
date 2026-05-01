package com.medical.medcore.controller;

import com.medical.medcore.entity.Doctor;
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

    @GetMapping
    public ResponseEntity<ApiResponse<PageableResponse<Doctor>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new ApiResponse<>(true, doctorService.findAll(page, size), "Médicos"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Doctor>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(true, doctorService.findById(id), "Médico"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Doctor>> create(@RequestBody Doctor doctor) {
        return ResponseEntity.ok(new ApiResponse<>(true, doctorService.create(doctor), "Médico creado"));
    }
}
