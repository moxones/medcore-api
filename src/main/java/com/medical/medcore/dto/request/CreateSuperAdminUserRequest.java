package com.medical.medcore.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateSuperAdminUserRequest {

    @NotNull
    private Long tenantId;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    @NotNull
    @Valid
    private PersonRequest person;

    private List<Long> roleIds;

    private List<String> roles;
}
