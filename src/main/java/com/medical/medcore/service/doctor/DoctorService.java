package com.medical.medcore.service.doctor;

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
        return PageableResponse.from(doctorRepository.findByTenantIdAndIsActiveTrue(tenantId, PageRequest.of(page, size)));
    }

    public Doctor findById(Long id) {
        return doctorRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new RuntimeException("Doctor no encontrado"));
    }

    public Doctor create(Doctor doctor) {
        doctor.setTenantId(TenantContext.getTenantId());
        return doctorRepository.save(doctor);
    }
}
