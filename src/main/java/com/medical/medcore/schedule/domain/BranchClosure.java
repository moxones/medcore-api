package com.medical.medcore.schedule.domain;

import com.medical.medcore.branch.domain.Branch;
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

@Entity
@Table(name = "branch_closures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchClosure extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(name = "closure_date", nullable = false)
    private LocalDate closureDate;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private ClosureSource source = ClosureSource.MANUAL;

    public Long branchId() {
        return branch != null ? branch.getId() : null;
    }

    public boolean appliesToBranch(Long branchId) {
        return branch == null || branch.getId().equals(branchId);
    }
}
