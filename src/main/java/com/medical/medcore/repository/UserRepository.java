package com.medical.medcore.repository;

import com.medical.medcore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndTenantId(String email, Long tenantId);

    List<User> findAllByTenantId(Long tenantId);

    Optional<User> findByIdAndTenantId(Long id, Long tenantId);
}