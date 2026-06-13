package com.medical.medcore.dto.request;

import com.medical.medcore.entity.enums.ClinicProcess;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Activa/desactiva procesos opcionales de la cola por clínica.
 * Solo se aplican las claves presentes; las omitidas conservan su valor actual.
 * Ejemplo: {@code { "processes": { "PAYMENT": false, "CALLED": true } }}.
 */
@Getter
@Setter
public class UpdateProcessConfigRequest {

    @NotNull
    private Map<ClinicProcess, Boolean> processes;
}
