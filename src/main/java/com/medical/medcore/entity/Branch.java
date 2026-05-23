package com.medical.medcore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(length = 20)
    private String ruc;

    @Column(name = "opening_time")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime openingTime;

    @Column(name = "closing_time")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime closingTime;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "appointment_duration_minutes")
    private Integer appointmentDurationMinutes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (appointmentDurationMinutes == null) appointmentDurationMinutes = 30;
        if (address == null) address = "";
    }
}
