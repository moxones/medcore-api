package com.medical.medcore.repository;

import com.medical.medcore.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserIdAndTenantIdAndIsRevokedFalse(Long userId, Long tenantId);

    @Modifying
    @Query("delete from RefreshToken rt where rt.expiresAt < :cutoff or rt.isRevoked = true")
    int deleteExpiredOrRevoked(@Param("cutoff") LocalDateTime cutoff);
}