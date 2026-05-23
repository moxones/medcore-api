package com.medical.medcore.service.doctor;

import com.medical.medcore.config.exception.BadRequestException;
import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.response.CatalogItemResponse;
import com.medical.medcore.entity.Doctor;
import com.medical.medcore.entity.DoctorSpecialty;
import com.medical.medcore.entity.Specialty;
import com.medical.medcore.repository.DoctorRepository;
import com.medical.medcore.repository.DoctorSpecialtyRepository;
import com.medical.medcore.repository.SpecialtyRepository;
import com.medical.medcore.repository.TenantSpecialtyRepository;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gestión de las especialidades asignadas a un doctor.
 * Una clínica solo puede asignar a sus doctores especialidades que tenga ACTIVADAS
 * (tenant_specialties), garantizando coherencia con el catálogo maestro.
 */
@Service
@RequiredArgsConstructor
public class DoctorSpecialtyService {

    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;
    private final DoctorSpecialtyRepository doctorSpecialtyRepository;
    private final TenantSpecialtyRepository tenantSpecialtyRepository;

    private Doctor requireDoctorInTenant(Long doctorId) {
        Long tenantId = TenantContext.requireTenantId();
        return doctorRepository.findByIdAndTenantId(doctorId, tenantId)
                .orElseThrow(() -> new NotFoundException("Doctor no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<CatalogItemResponse> listForDoctor(Long doctorId) {
        requireDoctorInTenant(doctorId);
        return doctorSpecialtyRepository.findByDoctorIdFetchSpecialty(doctorId).stream()
                .map(ds -> {
                    Specialty s = ds.getSpecialty();
                    return new CatalogItemResponse(
                            s.getId(), s.getCode(), s.getName(), s.getDescription(),
                            null, s.getIsActive(), true, true, ds.getId());
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CatalogItemResponse> listAvailableForDoctor(Long doctorId) {
        requireDoctorInTenant(doctorId);
        Long tenantId = TenantContext.requireTenantId();
        Set<Long> assigned = doctorSpecialtyRepository.findByDoctorIdFetchSpecialty(doctorId).stream()
                .map(ds -> ds.getSpecialty().getId())
                .collect(Collectors.toSet());
        return tenantSpecialtyRepository.findByTenantIdFetchSpecialty(tenantId).stream()
                .map(com.medical.medcore.entity.TenantSpecialty::getSpecialty)
                .filter(s -> !assigned.contains(s.getId()))
                .map(s -> new CatalogItemResponse(
                        s.getId(), s.getCode(), s.getName(), s.getDescription(),
                        null, s.getIsActive(), false, null, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public CatalogItemResponse assign(Long doctorId, Long specialtyId) {
        Doctor doctor = requireDoctorInTenant(doctorId);
        Long tenantId = TenantContext.requireTenantId();

        Specialty specialty = specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new NotFoundException("Especialidad no encontrada"));

        if (!tenantSpecialtyRepository.existsByTenantIdAndSpecialty_Id(tenantId, specialtyId)) {
            throw new BadRequestException("La especialidad no está activada para esta clínica");
        }
        if (doctorSpecialtyRepository.existsByDoctor_IdAndSpecialty_Id(doctorId, specialtyId)) {
            throw new BadRequestException("El doctor ya tiene asignada esta especialidad");
        }

        DoctorSpecialty ds = DoctorSpecialty.builder()
                .doctor(doctor)
                .specialty(specialty)
                .build();
        ds = doctorSpecialtyRepository.save(ds);
        return new CatalogItemResponse(specialty.getId(), specialty.getCode(), specialty.getName(),
                specialty.getDescription(), null, specialty.getIsActive(), true, true, ds.getId());
    }

    @Transactional
    public void remove(Long doctorId, Long specialtyId) {
        requireDoctorInTenant(doctorId);
        DoctorSpecialty ds = doctorSpecialtyRepository
                .findByDoctor_IdAndSpecialty_Id(doctorId, specialtyId)
                .orElseThrow(() -> new NotFoundException("El doctor no tiene asignada esta especialidad"));
        doctorSpecialtyRepository.delete(ds);
    }

    @Transactional
    public List<CatalogItemResponse> bulkAssign(Long doctorId, List<Long> specialtyIds) {
        Doctor doctor = requireDoctorInTenant(doctorId);
        Long tenantId = TenantContext.requireTenantId();

        List<CatalogItemResponse> results = new java.util.ArrayList<>();
        for (Long specialtyId : specialtyIds) {
            try {
                Specialty specialty = specialtyRepository.findById(specialtyId)
                        .orElseThrow(() -> new NotFoundException("Especialidad no encontrada: " + specialtyId));

                if (!tenantSpecialtyRepository.existsByTenantIdAndSpecialty_Id(tenantId, specialtyId)) {
                    throw new BadRequestException("La especialidad no está activada para esta clínica: " + specialtyId);
                }
                if (doctorSpecialtyRepository.existsByDoctor_IdAndSpecialty_Id(doctorId, specialtyId)) {
                    throw new BadRequestException("El doctor ya tiene asignada esta especialidad: " + specialtyId);
                }

                DoctorSpecialty ds = DoctorSpecialty.builder()
                        .doctor(doctor)
                        .specialty(specialty)
                        .build();
                ds = doctorSpecialtyRepository.save(ds);
                results.add(new CatalogItemResponse(
                        specialty.getId(), specialty.getCode(), specialty.getName(),
                        specialty.getDescription(), null, specialty.getIsActive(), true, true, ds.getId()));
            } catch (Exception e) {
                throw new BadRequestException("Error asignando especialidad " + specialtyId + ": " + e.getMessage());
            }
        }
        return results;
    }

    @Transactional
    public List<CatalogItemResponse> replaceAll(Long doctorId, List<Long> specialtyIds) {
        Doctor doctor = requireDoctorInTenant(doctorId);
        Long tenantId = TenantContext.requireTenantId();

        List<DoctorSpecialty> existing = doctorSpecialtyRepository.findByDoctorIdFetchSpecialty(doctorId);
        doctorSpecialtyRepository.deleteAll(existing);

        List<CatalogItemResponse> results = new java.util.ArrayList<>();
        for (Long specialtyId : specialtyIds) {
            Specialty specialty = specialtyRepository.findById(specialtyId)
                    .orElseThrow(() -> new NotFoundException("Especialidad no encontrada: " + specialtyId));

            if (!tenantSpecialtyRepository.existsByTenantIdAndSpecialty_Id(tenantId, specialtyId)) {
                throw new BadRequestException("La especialidad no está activada para esta clínica: " + specialtyId);
            }

            DoctorSpecialty ds = DoctorSpecialty.builder()
                    .doctor(doctor)
                    .specialty(specialty)
                    .build();
            ds = doctorSpecialtyRepository.save(ds);
            results.add(new CatalogItemResponse(
                    specialty.getId(), specialty.getCode(), specialty.getName(),
                    specialty.getDescription(), null, specialty.getIsActive(), true, true, ds.getId()));
        }
        return results;
    }
}
