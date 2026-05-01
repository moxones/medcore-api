package com.medical.medcore.dto.request;

import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    private String phone;
    private String gender;
    private LocalDate birthDate;

    @Email(message = "Email inválido")
    private String contactEmail;
}