package com.medical.medcore.repository;

import com.medical.medcore.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    long countByStatus(String status);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(s.plan.maxUsers) FROM Subscription s WHERE s.status = 'ACTIVE'")
    Long sumMaxUsersFromActiveSubscriptions();

    Optional<Subscription> findFirstByTenantIdOrderByEndDateDesc(Long tenantId);
}
