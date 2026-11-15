package com.medical.medcore.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_logos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantLogo {

    @Id
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "logo_key", nullable = false, unique = true, length = 36)
    private String logoKey;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "content", nullable = false)
    private byte[] content;

    @Column(name = "size_bytes", nullable = false)
    private Integer sizeBytes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
