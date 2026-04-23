package com.medical.medcore.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateUserRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    private PersonRequest person;

    private List<Long> roleIds;
}