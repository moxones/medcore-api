package com.medical.medcore.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MedicalEntryResponse(
        Long id,
        Long appointmentId,
        String entryType,
        String chiefComplaint,
        String presentIllness,
        String reviewOfSystems,
        String physicalExamination,
        String assessment,
        String plan,
        String diagnosis,
        String treatment,
        String notes,
        LocalDate followUpAt,
        Boolean isLocked,
        Long signedBy,
        String signedByName,
        LocalDateTime signedAt,
        LocalDateTime createdAt,
        Long createdBy,
        String createdByName,
        List<PrescriptionResponse> prescriptions,
        List<DiagnosisItem> diagnoses,
        List<ProcedureItem> procedures,
        List<OrderItem> orders,
        List<CertificateItem> certificates
) {
    public record DiagnosisItem(Long id, Long cie10Id, String cie10Code, String description,
                                String diagnosisType, String diagnosisRank, String notes) {}

    public record ProcedureItem(Long id, String code, String name, String notes, LocalDateTime performedAt) {}

    public record OrderItem(Long id, String orderType, String description, String status,
                            LocalDateTime requestedAt, List<OrderResultItem> results) {}

    public record OrderResultItem(Long id, String result, String fileUrl, LocalDateTime resultDate) {}

    public record CertificateItem(Long id, String certificateType, String content, Integer restDays,
                                  LocalDateTime issuedAt, LocalDate validUntil) {}
}
