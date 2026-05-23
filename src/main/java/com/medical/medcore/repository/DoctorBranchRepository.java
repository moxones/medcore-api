package com.medical.medcore.repository;

import com.medical.medcore.entity.DoctorBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorBranchRepository extends JpaRepository<DoctorBranch, Long> {

    @Query("SELECT db FROM DoctorBranch db JOIN FETCH db.branch WHERE db.doctor.id = :doctorId AND db.isActive = true")
    List<DoctorBranch> findActiveBranchesByDoctorId(Long doctorId);

    @Query("SELECT db FROM DoctorBranch db JOIN FETCH db.branch WHERE db.doctor.id IN :doctorIds AND db.isActive = true")
    List<DoctorBranch> findActiveBranchesByDoctorIdIn(@Param("doctorIds") List<Long> doctorIds);

    @Query("SELECT db FROM DoctorBranch db JOIN FETCH db.doctor JOIN FETCH db.doctor.person JOIN FETCH db.branch WHERE db.branch.id = :branchId AND db.isActive = true")
    List<DoctorBranch> findActiveDoctorsByBranchId(Long branchId);

    Optional<DoctorBranch> findByDoctor_IdAndBranch_Id(Long doctorId, Long branchId);

    @Query("SELECT db FROM DoctorBranch db JOIN FETCH db.doctor JOIN FETCH db.branch WHERE db.id = :id")
    Optional<DoctorBranch> findByIdFetchAll(@Param("id") Long id);

    boolean existsByDoctor_IdAndBranch_Id(Long doctorId, Long branchId);

    boolean existsByDoctor_IdAndBranch_IdAndIsActiveTrue(Long doctorId, Long branchId);
}
