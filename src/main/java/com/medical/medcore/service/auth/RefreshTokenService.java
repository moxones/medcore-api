package com.medical.medcore.service.auth;

import com.medical.medcore.entity.RefreshToken;

public interface RefreshTokenService {

    String create(Long userId, Long tenantId);

    RefreshToken validate(String rawToken);

    void revoke(RefreshToken token);

    /**
     * Acorta la expiración del token a una ventana de gracia corta en lugar de
     * revocarlo de inmediato. Se usa en la rotación de refresh para tolerar
     * requests concurrentes del mismo cliente.
     */
    void revokeWithGrace(RefreshToken token);

    void revokeAllByUserIdAndTenantId(Long userId, Long tenantId);
}