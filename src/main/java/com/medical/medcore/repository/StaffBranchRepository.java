package com.medical.medcore.repository;

import com.medical.medcore.entity.StaffBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffBranchRepository extends JpaRepository<StaffBranch, Long> {

    @Query("SELECT sb.branch.id FROM StaffBranch sb WHERE sb.user.id = :userId AND sb.isActive = true")
    List<Long> findActiveBranchIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT sb FROM StaffBranch sb JOIN FETCH sb.branch WHERE sb.user.id = :userId AND sb.isActive = true")
    List<StaffBranch> findActiveBranchesByUserId(@Param("userId") Long userId);

    @Query("SELECT sb FROM StaffBranch sb JOIN FETCH sb.branch WHERE sb.user.id = :userId")
    List<StaffBranch> findBranchesByUserId(@Param("userId") Long userId);

    Optional<StaffBranch> findByUser_IdAndBranch_Id(Long userId, Long branchId);

    boolean existsByUser_IdAndBranch_IdAndIsActiveTrue(Long userId, Long branchId);
}
