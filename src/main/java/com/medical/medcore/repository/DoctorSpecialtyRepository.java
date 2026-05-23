package com.medical.medcore.repository;

import com.medical.medcore.entity.DoctorSpecialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorSpecialtyRepository extends JpaRepository<DoctorSpecialty, Long> {

    @Query("SELECT ds FROM DoctorSpecialty ds JOIN FETCH ds.specialty WHERE ds.doctor.id = :doctorId")
    List<DoctorSpecialty> findByDoctorIdFetchSpecialty(Long doctorId);

    @Query("SELECT ds FROM DoctorSpecialty ds JOIN FETCH ds.specialty WHERE ds.doctor.id IN :doctorIds")
    List<DoctorSpecialty> findByDoctorIdIn(@Param("doctorIds") List<Long> doctorIds);

    Optional<DoctorSpecialty> findByDoctor_IdAndSpecialty_Id(Long doctorId, Long specialtyId);

    boolean existsByDoctor_IdAndSpecialty_Id(Long doctorId, Long specialtyId);
}
