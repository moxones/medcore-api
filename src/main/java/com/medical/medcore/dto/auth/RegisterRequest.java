package com.medical.medcore.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    private String firstName;
    private String lastName;
    private LocalDate birthDate;

    private String documentTypeCode;
    private String documentNumber;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password debe tener al menos 8 caracteres")
    private String password;
}