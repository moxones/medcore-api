package com.medical.medcore.controller;

import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.entity.MedicalRecord;
import com.medical.medcore.repository.MedicalRecordRepository;
import com.medical.medcore.types.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    // CRUD super simplificado directamente con repository por solicitud de rapidez
    private final MedicalRecordRepository recordRepository;

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<MedicalRecord>> findByPatient(@PathVariable Long patientId) {
        MedicalRecord record = recordRepository.findByPatientId(patientId)
                .orElseThrow(() -> new NotFoundException("Historial clínico no encontrado"));
        return ResponseEntity.ok(new ApiResponse<>(true, record, "Historial clínico"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MedicalRecord>> create(@RequestBody MedicalRecord record) {
        return ResponseEntity.ok(new ApiResponse<>(true, recordRepository.save(record), "Historial clínico creado"));
    }
}
