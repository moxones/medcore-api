package com.medical.medcore.platform.repository;

import com.medical.medcore.platform.domain.TenantLogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantLogoRepository extends JpaRepository<TenantLogo, Long> {

    Optional<TenantLogo> findByLogoKey(String logoKey);
}
