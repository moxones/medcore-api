package com.medical.medcore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "medical_certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_entry_id", nullable = false)
    private MedicalEntry medicalEntry;

    @Column(name = "certificate_type", nullable = false, length = 30)
    private String certificateType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "rest_days")
    private Integer restDays;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "created_by")
    private Long createdBy;

    @PrePersist
    protected void onCreate() {
        if (issuedAt == null) issuedAt = LocalDateTime.now();
    }
}
