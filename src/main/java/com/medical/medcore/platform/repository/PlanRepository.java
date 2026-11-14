package com.medical.medcore.platform.repository;

import com.medical.medcore.platform.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByIsActiveTrue();

    boolean existsByCode(String code);
}
