package com.medical.medcore.platform.repository;

import com.medical.medcore.platform.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    long countByStatus(String status);

    @Query("SELECT SUM(s.plan.maxUsers) FROM Subscription s WHERE s.status = 'ACTIVE'")
    Long sumMaxUsersFromActiveSubscriptions();

    Optional<Subscription> findFirstByTenantIdOrderByEndDateDesc(Long tenantId);

    Optional<Subscription> findFirstByTenantIdOrderByStartDateDescIdDesc(Long tenantId);

    Optional<Subscription> findByIdAndTenantId(Long id, Long tenantId);

    List<Subscription> findByTenantIdOrderByStartDateDescIdDesc(Long tenantId);

    List<Subscription> findAllByOrderByTenantIdAscStartDateDescIdDesc();

    boolean existsByTenantIdAndStatusAndIdNot(Long tenantId, String status, Long id);
}
