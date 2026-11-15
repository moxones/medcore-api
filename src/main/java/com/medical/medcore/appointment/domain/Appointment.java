package com.medical.medcore.appointment.domain;

import com.medical.medcore.branch.domain.Branch;
import com.medical.medcore.doctor.domain.Doctor;
import com.medical.medcore.patient.domain.Patient;
import com.medical.medcore.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment extends AuditableEntity {

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

    @Builder.Default
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 30;

    @Builder.Default
    @Column(name = "flow_status", nullable = false, length = 50)
    private String flowStatus = AppointmentFlowStatus.SCHEDULED.name();

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "called_at")
    private LocalDateTime calledAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_source", length = 30)
    private BookingSource bookingSource;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @Column(name = "no_show_at")
    private LocalDateTime noShowAt;

    @Builder.Default
    @Column(name = "needs_reschedule", nullable = false)
    private Boolean needsReschedule = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "reschedule_cause", length = 30)
    private RescheduleCause rescheduleCause;

    @Column(name = "reschedule_note", length = 255)
    private String rescheduleNote;

    @Column(name = "reschedule_flagged_at")
    private LocalDateTime rescheduleFlaggedAt;

    @Column(name = "schedule_exception_id")
    private Long scheduleExceptionId;

    @Column(name = "branch_closure_id")
    private Long branchClosureId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_origin", length = 20)
    private CancellationOrigin cancellationOrigin;

    public AppointmentFlowStatus flow() {
        return AppointmentFlowStatus.from(flowStatus);
    }

    public LocalDateTime endsAt() {
        return scheduledAt.plusMinutes(durationMinutes);
    }

    public void moveTo(AppointmentFlowStatus target, LocalDateTime moment) {
        switch (target) {
            case WAITING -> checkedInAt = firstNonNull(checkedInAt, moment);
            case CALLED -> calledAt = firstNonNull(calledAt, moment);
            case IN_PROCESS -> startedAt = firstNonNull(startedAt, moment);
            case PENDING_PAYMENT -> finishedAt = firstNonNull(finishedAt, moment);
            case COMPLETED -> {
                finishedAt = firstNonNull(finishedAt, moment);
                completedAt = firstNonNull(completedAt, moment);
            }
            case SCHEDULED -> throw new IllegalArgumentException("SCHEDULED no es un destino de flujo");
        }
        flowStatus = target.name();
    }

    public void cancel(Long cancelledStatusId, String reasonText, CancellationOrigin origin, Long userId,
                       LocalDateTime moment) {
        statusId = cancelledStatusId;
        cancelReason = reasonText;
        cancellationOrigin = origin;
        cancelledAt = moment;
        cancelledBy = userId;
        needsReschedule = false;
    }

    public boolean isFlaggedForReschedule() {
        return Boolean.TRUE.equals(needsReschedule);
    }

    public void flagForReschedule(RescheduleCause cause, String note, Long exceptionId, Long closureId,
                                  LocalDateTime moment) {
        needsReschedule = true;
        rescheduleCause = cause;
        rescheduleNote = note != null && note.length() > 255 ? note.substring(0, 255) : note;
        rescheduleFlaggedAt = moment;
        scheduleExceptionId = exceptionId;
        branchClosureId = closureId;
    }

    public void resolveRescheduleFlag() {
        needsReschedule = false;
    }

    public void withdrawRescheduleFlag() {
        needsReschedule = false;
        rescheduleCause = null;
        rescheduleNote = null;
        rescheduleFlaggedAt = null;
        scheduleExceptionId = null;
        branchClosureId = null;
    }

    public CancellationOrigin defaultCancellationOrigin() {
        if (!isFlaggedForReschedule() || rescheduleCause == null) {
            return CancellationOrigin.PATIENT;
        }
        return rescheduleCause == RescheduleCause.BRANCH_CLOSURE
                ? CancellationOrigin.ORGANIZATION
                : CancellationOrigin.PROFESSIONAL;
    }

    public void returnToSchedule() {
        flowStatus = AppointmentFlowStatus.SCHEDULED.name();
        checkedInAt = null;
        calledAt = null;
    }

    public void markNoShow(Long noShowStatusId, LocalDateTime moment) {
        statusId = noShowStatusId;
        noShowAt = moment;
    }

    private static LocalDateTime firstNonNull(LocalDateTime current, LocalDateTime fallback) {
        return current != null ? current : fallback;
    }
}
