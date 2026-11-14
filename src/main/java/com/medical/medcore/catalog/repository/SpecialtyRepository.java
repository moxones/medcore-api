package com.medical.medcore.catalog.repository;

import com.medical.medcore.catalog.domain.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {
    List<Specialty> findByIsActiveTrue();
    Optional<Specialty> findByCode(String code);
    boolean existsByCode(String code);
}
