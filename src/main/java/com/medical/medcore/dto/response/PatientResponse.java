package com.medical.medcore.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
public class PatientResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String contactEmail;
}