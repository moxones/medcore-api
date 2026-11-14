package com.medical.medcore.triage.domain;

import com.medical.medcore.appointment.domain.Appointment;
import com.medical.medcore.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "triage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Triage extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(precision = 5, scale = 2)
    private BigDecimal height;

    @Column(precision = 4, scale = 2)
    private BigDecimal temperature;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "respiratory_rate")
    private Integer respiratoryRate;

    @Column(name = "oxygen_saturation", precision = 5, scale = 2)
    private BigDecimal oxygenSaturation;

    @Column(name = "blood_pressure", length = 20)
    private String bloodPressure;

    @Column(name = "systolic_pressure")
    private Integer systolicPressure;

    @Column(name = "diastolic_pressure")
    private Integer diastolicPressure;

    @Column(name = "pain_scale")
    private Integer painScale;

    @Column(name = "blood_glucose", precision = 5, scale = 2)
    private BigDecimal bloodGlucose;

    @Column(precision = 5, scale = 2)
    private BigDecimal bmi;

    @Column(name = "priority_level", length = 20)
    private String priorityLevel;

    @Column(name = "priority_system", length = 20)
    private String prioritySystem;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "measured_at")
    private LocalDateTime measuredAt;
}
