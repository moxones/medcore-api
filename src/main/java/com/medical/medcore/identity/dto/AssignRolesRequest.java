package com.medical.medcore.identity.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AssignRolesRequest(@NotEmpty(message = "Debe enviar al menos un rol") List<Long> roleIds) {
}
