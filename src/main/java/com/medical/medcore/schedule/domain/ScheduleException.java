package com.medical.medcore.schedule.domain;

import com.medical.medcore.branch.domain.Branch;
import com.medical.medcore.doctor.domain.Doctor;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "schedule_exceptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleException extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", insertable = false, updatable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 10)
    private ExceptionKind kind;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "slot_duration_minutes")
    private Integer slotDurationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private ExceptionReason reason;

    @Column(name = "note", length = 255)
    private String note;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private ExceptionStatus status = ExceptionStatus.APPROVED;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 255)
    private String reviewNote;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public boolean isFullDay() {
        return startTime == null;
    }

    public boolean isBlock() {
        return kind == ExceptionKind.BLOCK;
    }

    public boolean isApproved() {
        return status == ExceptionStatus.APPROVED;
    }

    public boolean covers(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public boolean appliesToBranch(Long branchId) {
        return branch == null || branch.getId().equals(branchId);
    }

    public Long branchId() {
        return branch != null ? branch.getId() : null;
    }

    public boolean overlapsTime(LocalTime start, LocalTime end) {
        if (isFullDay()) {
            return true;
        }
        return startTime.isBefore(end) && start.isBefore(endTime);
    }

    public boolean overlapsDates(LocalDate from, LocalDate to) {
        return !startDate.isAfter(to) && !from.isAfter(endDate);
    }

    public long days() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public void review(ExceptionStatus outcome, Long userId, String noteText, LocalDateTime moment) {
        status = outcome;
        reviewedBy = userId;
        reviewedAt = moment;
        reviewNote = noteText;
    }

    public void cancel(Long userId, LocalDateTime moment) {
        status = ExceptionStatus.CANCELLED;
        cancelledBy = userId;
        cancelledAt = moment;
    }
}
