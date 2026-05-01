package com.medical.medcore.service.patient;

import com.medical.medcore.dto.request.CreatePatientRequest;
import com.medical.medcore.dto.request.UpdateProfileRequest;
import com.medical.medcore.dto.response.PatientResponse;

import java.util.List;

public interface PatientService {

    PatientResponse create(CreatePatientRequest request);

    List<PatientResponse> findAll();

    PatientResponse findById(Long id);

    void updateProfile(UpdateProfileRequest request);

    List<PatientResponse> search(String term);
}