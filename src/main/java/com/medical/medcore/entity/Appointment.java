package com.medical.medcore.entity;

import com.medical.medcore.entity.enums.BookingSource;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "status_id", nullable = false)
    private Long statusId;

    @Column(name = "appointment_type_id")
    private Long appointmentTypeId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "flow_status", nullable = false, length = 50)
    private String flowStatus;

    // Timestamps por transición del flujo operativo (base de los timers del board).
    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;   // pasó a WAITING (check-in)

    @Column(name = "called_at")
    private LocalDateTime calledAt;      // pasó a CALLED

    @Column(name = "started_at")
    private LocalDateTime startedAt;     // pasó a IN_PROCESS

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;    // pasó a PENDING_PAYMENT (o COMPLETED si se saltó)

    @Column(name = "completed_at")
    private LocalDateTime completedAt;   // pasó a COMPLETED

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_source", length = 30)
    private BookingSource bookingSource;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (durationMinutes == null) durationMinutes = 30;
        if (flowStatus == null) flowStatus = "SCHEDULED";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
