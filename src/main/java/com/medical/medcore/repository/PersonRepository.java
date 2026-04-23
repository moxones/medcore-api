package com.medical.medcore.repository;

import com.medical.medcore.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {

    Optional<Person> findByIdAndTenantId(Long id, Long tenantId);

    List<Person> findAllByTenantId(Long tenantId);
}