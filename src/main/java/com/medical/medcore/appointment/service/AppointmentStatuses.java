package com.medical.medcore.appointment.service;

import com.medical.medcore.appointment.domain.AppointmentStatusCode;
import com.medical.medcore.catalog.domain.AppointmentStatus;
import com.medical.medcore.catalog.repository.AppointmentStatusRepository;
import com.medical.medcore.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class AppointmentStatuses {

    private final AppointmentStatusRepository repository;
    private final Map<String, Long> idsByCode = new ConcurrentHashMap<>();
    private final Map<Long, String> codesById = new ConcurrentHashMap<>();

    public Long idOf(AppointmentStatusCode code) {
        Long id = idsByCode.get(code.name());
        if (id != null) {
            return id;
        }
        reload();
        Long reloaded = idsByCode.get(code.name());
        if (reloaded == null) {
            throw new IllegalStateException("Estado de cita no configurado en el catálogo: " + code.name());
        }
        return reloaded;
    }

    public String codeOf(Long statusId) {
        if (statusId == null) {
            return null;
        }
        String code = codesById.get(statusId);
        if (code != null) {
            return code;
        }
        reload();
        return codesById.get(statusId);
    }

    public boolean is(Long statusId, AppointmentStatusCode code) {
        return code.name().equals(codeOf(statusId));
    }

    public List<Long> inactiveIds() {
        return List.of(idOf(AppointmentStatusCode.CANCELLED), idOf(AppointmentStatusCode.NO_SHOW));
    }

    public Long resolveFilter(Long statusId, String statusCode) {
        if (statusId != null) {
            return statusId;
        }
        if (statusCode == null || statusCode.isBlank()) {
            return null;
        }
        try {
            return idOf(AppointmentStatusCode.valueOf(statusCode.trim().toUpperCase()));
        } catch (IllegalArgumentException unknown) {
            throw new BadRequestException("Estado de cita inválido: " + statusCode);
        }
    }

    private synchronized void reload() {
        for (AppointmentStatus status : repository.findAll()) {
            idsByCode.put(status.getCode().toUpperCase(), status.getId());
            codesById.put(status.getId(), status.getCode().toUpperCase());
        }
    }
}
