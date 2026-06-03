package com.medical.medcore.service.medicalrecord;

import com.medical.medcore.dto.request.CreateMedicalEntryRequest;
import com.medical.medcore.dto.request.UpdatePatientClinicalRequest;
import com.medical.medcore.dto.response.MedicalEntryResponse;
import com.medical.medcore.dto.response.MedicalRecordResponse;

import java.util.List;

public interface MedicalRecordService {

    MedicalRecordResponse getByPatientId(Long patientId);

    MedicalRecordResponse getMyRecord();

    MedicalEntryResponse addEntry(CreateMedicalEntryRequest request);

    MedicalEntryResponse getEntry(Long entryId);

    List<MedicalEntryResponse> getEntriesByAppointment(Long appointmentId);

    MedicalRecordResponse updatePatientClinical(Long patientId, UpdatePatientClinicalRequest request);
}
