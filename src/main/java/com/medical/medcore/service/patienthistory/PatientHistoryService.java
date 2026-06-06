package com.medical.medcore.service.patienthistory;

import com.medical.medcore.dto.request.PatientAllergyRequest;
import com.medical.medcore.dto.request.PatientConditionRequest;
import com.medical.medcore.dto.request.PatientFamilyHistoryRequest;
import com.medical.medcore.dto.request.PatientHabitRequest;
import com.medical.medcore.dto.request.PatientSurgicalHistoryRequest;
import com.medical.medcore.dto.response.PatientClinicalHistoryResponse;
import com.medical.medcore.dto.response.PatientClinicalHistoryResponse.*;

public interface PatientHistoryService {

    PatientClinicalHistoryResponse getHistory(Long patientId);

    AllergyItem addAllergy(Long patientId, PatientAllergyRequest request);

    void deleteAllergy(Long patientId, Long id);

    ConditionItem addCondition(Long patientId, PatientConditionRequest request);

    void deleteCondition(Long patientId, Long id);

    FamilyHistoryItem addFamilyHistory(Long patientId, PatientFamilyHistoryRequest request);

    void deleteFamilyHistory(Long patientId, Long id);

    SurgicalHistoryItem addSurgicalHistory(Long patientId, PatientSurgicalHistoryRequest request);

    void deleteSurgicalHistory(Long patientId, Long id);

    HabitItem addHabit(Long patientId, PatientHabitRequest request);

    void deleteHabit(Long patientId, Long id);
}
