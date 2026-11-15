package com.medical.medcore.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Código enviado para confirmar un correo antes de crear la cuenta (registro web del paciente). Todavía no hay
 * usuario, por eso se guarda por organización y correo. {@code delivered = false} recuerda que el correo no salió:
 * en ese caso el registro puede continuar sin código.
 */
@Entity
@Table(name = "email_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Builder.Default
    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Builder.Default
    @Column(name = "delivered", nullable = false)
    private Boolean delivered = false;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "requested_ip", length = 64)
    private String requestedIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isUsableAt(LocalDateTime moment, int maxAttempts) {
        return consumedAt == null && expiresAt.isAfter(moment) && attempts < maxAttempts;
    }

    public boolean wasDelivered() {
        return Boolean.TRUE.equals(delivered);
    }

    public void consume(LocalDateTime moment) {
        consumedAt = moment;
    }
}
