package com.medical.medcore.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PersonRequest {

    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String gender;
    private String phone;

    private String documentTypeCode;
    private String documentNumber;
}