package com.medical.medcore.repository;

import com.medical.medcore.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionStatusRepository extends JpaRepository<SubscriptionStatus, Long> {
}
