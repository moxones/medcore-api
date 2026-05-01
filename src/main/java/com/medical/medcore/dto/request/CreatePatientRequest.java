package com.medical.medcore.dto.request;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
public class CreatePatientRequest {

    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String documentTypeCode;
    private String documentNumber;
}