package com.medical.medcore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Configuración por clínica de qué procesos/etapas de la cola están activos.
 * Patrón pivote por tenant (igual que {@link TenantSpecialty}): una fila por
 * (tenant, proceso). La ausencia de fila se interpreta como proceso activo.
 */
@Entity
@Table(name = "tenant_process_config",
        uniqueConstraints = @UniqueConstraint(name = "uq_tenant_process",
                columnNames = {"tenant_id", "process"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantProcessConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    /** Nombre del enum {@link com.medical.medcore.entity.enums.ClinicProcess}. */
    @Column(name = "process", nullable = false, length = 50)
    private String process;

    @Column(name = "is_active")
    private Boolean isActive;

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
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
