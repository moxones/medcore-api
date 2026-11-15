package com.medical.medcore.identity.repository;

import com.medical.medcore.identity.domain.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findFirstByTenantIdAndEmailOrderByCreatedAtDescIdDesc(Long tenantId, String email);

    @Modifying
    @Query("UPDATE EmailVerification v SET v.expiresAt = :moment "
            + "WHERE v.tenantId = :tenantId AND v.email = :email AND v.consumedAt IS NULL AND v.expiresAt > :moment")
    int expirePending(@Param("tenantId") Long tenantId, @Param("email") String email,
                      @Param("moment") LocalDateTime moment);

    @Modifying
    @Query("UPDATE EmailVerification v SET v.attempts = v.attempts + 1 WHERE v.id = :id")
    int registerFailedAttempt(@Param("id") Long id);

    @Modifying
    @Query("DELETE FROM EmailVerification v WHERE v.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") LocalDateTime cutoff);
}
