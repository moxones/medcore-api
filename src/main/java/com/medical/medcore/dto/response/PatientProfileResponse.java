package com.medical.medcore.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
public class PatientProfileResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private String gender;
    private String birthDate;
    private String contactEmail;
    private boolean profileCompleted;
}