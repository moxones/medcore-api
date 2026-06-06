package com.medical.medcore.dto.response;

import java.time.LocalDate;
import java.util.List;

public record PatientClinicalHistoryResponse(
        Long patientId,
        String bloodType,
        List<AllergyItem> allergies,
        List<ConditionItem> conditions,
        List<FamilyHistoryItem> familyHistory,
        List<SurgicalHistoryItem> surgicalHistory,
        List<HabitItem> habits
) {
    public record AllergyItem(Long id, String allergen, String reaction, String severity, Boolean isActive) {}

    public record ConditionItem(Long id, Long cie10Id, String cie10Code, String description,
                                String status, LocalDate diagnosedAt) {}

    public record FamilyHistoryItem(Long id, String relationship, String condition, String notes) {}

    public record SurgicalHistoryItem(Long id, String procedure, LocalDate performedOn, String notes) {}

    public record HabitItem(Long id, String habitType, String detail, String status) {}
}
