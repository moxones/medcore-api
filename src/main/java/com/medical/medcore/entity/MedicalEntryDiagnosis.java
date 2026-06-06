package com.medical.medcore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "medical_entry_diagnoses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalEntryDiagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_entry_id", nullable = false)
    private MedicalEntry medicalEntry;

    @Column(name = "cie10_id")
    private Long cie10Id;

    @Column(nullable = false, length = 300)
    private String description;

    @Column(name = "diagnosis_type", nullable = false, length = 20)
    private String diagnosisType;

    @Column(name = "diagnosis_rank", nullable = false, length = 20)
    private String diagnosisRank;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (diagnosisType == null) diagnosisType = "DEFINITIVE";
        if (diagnosisRank == null) diagnosisRank = "PRIMARY";
    }
}
