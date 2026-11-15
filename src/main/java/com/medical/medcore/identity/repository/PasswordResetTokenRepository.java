package com.medical.medcore.identity.repository;

import com.medical.medcore.identity.domain.PasswordResetToken;
import com.medical.medcore.identity.domain.TokenPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findFirstByUserIdAndPurposeOrderByCreatedAtDescIdDesc(Long userId, TokenPurpose purpose);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.usedAt = :moment WHERE t.userId = :userId AND t.usedAt IS NULL")
    int invalidatePending(@Param("userId") Long userId, @Param("moment") LocalDateTime moment);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.usedAt = :moment "
            + "WHERE t.userId = :userId AND t.purpose = :purpose AND t.usedAt IS NULL")
    int invalidatePending(@Param("userId") Long userId, @Param("purpose") TokenPurpose purpose,
                          @Param("moment") LocalDateTime moment);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") LocalDateTime cutoff);
}
