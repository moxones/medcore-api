package com.medical.medcore.identity.repository;

import com.medical.medcore.identity.domain.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {

    Optional<Person> findByIdAndTenantId(Long id, Long tenantId);

    List<Person> findAllByTenantId(Long tenantId);

    @Modifying
    @Query(value = """
            UPDATE persons
               SET identity_check_failures = CASE WHEN identity_check_failures + 1 >= :maxAttempts
                                                  THEN 0 ELSE identity_check_failures + 1 END,
                   identity_check_locked_until = CASE WHEN identity_check_failures + 1 >= :maxAttempts
                                                      THEN :lockUntil ELSE identity_check_locked_until END
             WHERE id = :personId
            """, nativeQuery = true)
    int registerIdentityCheckFailure(@Param("personId") Long personId,
                                     @Param("maxAttempts") int maxAttempts,
                                     @Param("lockUntil") LocalDateTime lockUntil);
}
