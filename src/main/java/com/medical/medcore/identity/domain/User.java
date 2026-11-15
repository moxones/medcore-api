package com.medical.medcore.identity.domain;

import com.medical.medcore.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Builder.Default
    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Builder.Default
    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword = false;

    @Builder.Default
    @Column(name = "pending_activation", nullable = false)
    private Boolean pendingActivation = false;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    public boolean isLockedAt(LocalDateTime moment) {
        return lockedUntil != null && lockedUntil.isAfter(moment);
    }

    public void registerFailedLogin(int maxAttempts, LocalDateTime lockUntil) {
        failedLoginAttempts = (failedLoginAttempts == null ? 0 : failedLoginAttempts) + 1;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = lockUntil;
            failedLoginAttempts = 0;
        }
    }

    public void registerSuccessfulLogin(LocalDateTime moment) {
        failedLoginAttempts = 0;
        lockedUntil = null;
        lastLoginAt = moment;
    }

    public void changePassword(String encodedPassword, LocalDateTime moment, boolean mustChangeOnNextLogin) {
        password = encodedPassword;
        passwordChangedAt = moment;
        mustChangePassword = mustChangeOnNextLogin;
        pendingActivation = false;
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    public boolean isPendingActivation() {
        return Boolean.TRUE.equals(pendingActivation);
    }

    public boolean hasRole(String code) {
        return roles != null && roles.stream().anyMatch(role -> code.equals(role.getCode()));
    }
}
