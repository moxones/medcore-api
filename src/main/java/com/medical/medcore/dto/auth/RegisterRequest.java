package com.medical.medcore.dto.auth;

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

    private String email;
    private String password;
}