package com.medical.medcore.service.doctor;

import com.medical.medcore.config.exception.BadRequestException;
import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.entity.Doctor;
import com.medical.medcore.repository.DoctorRepository;
import com.medical.medcore.types.PageableResponse;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;

    public PageableResponse<Doctor> findAll(int page, int size) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BadRequestException("Tenant no disponible");
        }
        return PageableResponse.from(doctorRepository.findByTenantIdAndIsActiveTrue(tenantId, PageRequest.of(page, size)));
    }

    public Doctor findById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BadRequestException("Tenant no disponible");
        }
        return doctorRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Doctor no encontrado"));
    }

    public Doctor create(Doctor doctor) {
        Long tenantId = TenantContext.requireTenantId();
        doctor.setTenantId(tenantId);
        return doctorRepository.save(doctor);
    }

    public Doctor update(Long id, Doctor doctor) {
        Long tenantId = TenantContext.requireTenantId();
        Doctor existing = doctorRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Médico no encontrado"));
        if (doctor.getLicenseNumber() != null) {
            existing.setLicenseNumber(doctor.getLicenseNumber());
        }
        if (doctor.getIsActive() != null) {
            existing.setIsActive(doctor.getIsActive());
        }
        return doctorRepository.save(existing);
    }
}
