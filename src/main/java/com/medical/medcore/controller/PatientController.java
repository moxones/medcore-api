package com.medical.medcore.controller;

import com.medical.medcore.dto.request.CreatePatientRequest;
import com.medical.medcore.dto.request.UpdateProfileRequest;
import com.medical.medcore.dto.response.PatientResponse;
import com.medical.medcore.service.patient.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    public PatientResponse create(@RequestBody CreatePatientRequest request) {
        return patientService.create(request);
    }

    @GetMapping
    public List<PatientResponse> findAll() {
        return patientService.findAll();
    }

    @GetMapping("/{id}")
    public PatientResponse findById(@PathVariable Long id) {
        return patientService.findById(id);
    }

    @PutMapping("/patients/profile")
    public void updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
        patientService.updateProfile(request);
    }

    @GetMapping("/patients/search")
    public List<PatientResponse> search(@RequestParam String term) {
        return patientService.search(term);
    }
}