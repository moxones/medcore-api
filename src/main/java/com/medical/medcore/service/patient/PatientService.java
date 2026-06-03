package com.medical.medcore.service.patient;

import com.medical.medcore.dto.request.CreatePatientRequest;
import com.medical.medcore.dto.request.UpdatePatientRequest;
import com.medical.medcore.dto.request.UpdateProfileRequest;
import com.medical.medcore.dto.response.AppointmentResponse;
import com.medical.medcore.dto.response.PatientProfileResponse;
import com.medical.medcore.dto.response.PatientResponse;
import com.medical.medcore.types.PageableResponse;

import java.time.LocalDate;
import java.util.List;

public interface PatientService {

    PatientResponse create(CreatePatientRequest request);

    PageableResponse<PatientResponse> findAll(int page, int size);

    PatientResponse findById(Long id);

    PatientProfileResponse getProfile();

    void updateProfile(UpdateProfileRequest request);

    PatientResponse updatePatient(Long id, UpdatePatientRequest request);

    List<PatientResponse> search(String term);

    PageableResponse<AppointmentResponse> getMyAppointments(int page, int size, Long statusId, LocalDate date, String flowStatus);
}