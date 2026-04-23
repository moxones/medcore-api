package com.medical.medcore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(name = "tenant_id", nullable = false)
    public Long tenantId;

    @Column(name = "token", nullable = false)
    public String token;

    @Column(name = "expires_at", nullable = false)
    public LocalDateTime expiresAt;

    @Column(name = "is_revoked", nullable = false)
    public Boolean isRevoked;


    public void revoke() { this.isRevoked = true; }
}