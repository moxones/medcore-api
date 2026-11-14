package com.medical.medcore.identity.repository;

import com.medical.medcore.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndTenantId(String email, Long tenantId);

    boolean existsByEmailAndTenantId(String email, Long tenantId);

    boolean existsByPersonIdAndTenantId(Long personId, Long tenantId);

    List<User> findAllByTenantIdOrderByIdAsc(Long tenantId);

    List<User> findAllByOrderByTenantIdAscIdAsc();

    Optional<User> findByIdAndTenantId(Long id, Long tenantId);

    Optional<User> findByPersonIdAndTenantId(Long personId, Long tenantId);

    List<User> findByPersonIdInAndTenantId(Collection<Long> personIds, Long tenantId);

    List<User> findByIdInAndTenantId(Collection<Long> ids, Long tenantId);

    long countByTenantIdAndIsActiveTrue(Long tenantId);

    long countByIsActiveTrue();

    @Query("SELECT COUNT(u) > 0 FROM User u JOIN u.roles r WHERE r.code = :roleCode")
    boolean existsByRoleCode(@Param("roleCode") String roleCode);
}
