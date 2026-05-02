package com.medical.medcore.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AssignRolesRequest {

    @NotEmpty(message = "Debe enviar al menos un rol")
    private List<Long> roleIds;
}