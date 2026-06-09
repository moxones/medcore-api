package com.medical.medcore.report.support;

import com.medical.medcore.entity.User;
import com.medical.medcore.report.ReportContext;
import com.medical.medcore.repository.DoctorRepository;
import com.medical.medcore.repository.PatientRepository;
import com.medical.medcore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Resuelve el médico/paciente correspondiente al usuario autenticado, para acotar los reportes
 * de rol DOCTOR/PATIENT estrictamente a sus propios datos (se ignora cualquier id del cliente).
 */
@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public Long resolveDoctorId(ReportContext ctx) {
        Long personId = personId(ctx);
        return doctorRepository.findByPersonIdAndTenantId(personId, ctx.tenantId())
                .orElseThrow(() -> new AccessDeniedException("El usuario no tiene un médico asociado"))
                .getId();
    }

    public Long resolvePatientId(ReportContext ctx) {
        Long personId = personId(ctx);
        return patientRepository.findByPersonIdAndTenantId(personId, ctx.tenantId())
                .orElseThrow(() -> new AccessDeniedException("El usuario no tiene un paciente asociado"))
                .getId();
    }

    private Long personId(ReportContext ctx) {
        if (ctx.userId() == null) {
            throw new AccessDeniedException("Usuario autenticado no resuelto");
        }
        User user = userRepository.findByIdAndTenantId(ctx.userId(), ctx.tenantId())
                .orElseThrow(() -> new AccessDeniedException("Usuario no encontrado"));
        if (user.getPerson() == null) {
            throw new AccessDeniedException("El usuario no tiene una persona asociada");
        }
        return user.getPerson().getId();
    }
}
