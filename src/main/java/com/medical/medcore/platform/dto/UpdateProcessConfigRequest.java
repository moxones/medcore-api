package com.medical.medcore.platform.dto;

import com.medical.medcore.platform.domain.ClinicProcess;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class UpdateProcessConfigRequest {

    @NotNull
    private Map<ClinicProcess, Boolean> processes;
}
