package com.medical.medcore.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private TokenPurpose purpose = TokenPurpose.PASSWORD_RESET;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "requested_ip", length = 64)
    private String requestedIp;

    @Builder.Default
    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isUsableAt(LocalDateTime moment) {
        return usedAt == null && expiresAt.isAfter(moment);
    }

    public boolean hasAttemptsLeft(int maxAttempts) {
        return attempts < maxAttempts;
    }

    /** Suma un intento fallido; al llegar al máximo el código queda inutilizable. */
    public void registerFailedAttempt(int maxAttempts, LocalDateTime moment) {
        attempts = attempts + 1;
        if (attempts >= maxAttempts) {
            usedAt = moment;
        }
    }

    public void markUsed(LocalDateTime moment) {
        usedAt = moment;
    }
}
