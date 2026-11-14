package com.medical.medcore.platform.repository;

import com.medical.medcore.platform.domain.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionStatusRepository extends JpaRepository<SubscriptionStatus, Long> {

    boolean existsByCode(String code);
}
