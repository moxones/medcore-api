package com.medical.medcore.repository;

import com.medical.medcore.entity.Cie10Code;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface Cie10CodeRepository extends JpaRepository<Cie10Code, Long> {

    @Query("""
            SELECT c FROM Cie10Code c
            WHERE c.isActive = true
              AND (:q IS NULL OR CAST(:q AS String) = ''
                   OR LOWER(c.code) LIKE LOWER(CONCAT(CAST(:q AS String), '%'))
                   OR LOWER(c.description) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%')))
            ORDER BY c.code ASC
            """)
    Page<Cie10Code> search(@Param("q") String q, Pageable pageable);
}
