package com.medical.medcore.service.auth;

import com.medical.medcore.entity.RefreshToken;

public interface RefreshTokenService {

    String create(Long userId, Long tenantId);

    RefreshToken validate(String rawToken);

    void revoke(RefreshToken token);

    void revokeAllByUserIdAndTenantId(Long userId, Long tenantId);
}