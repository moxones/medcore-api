package com.medical.medcore.dto.request;

public record UpdatePatientClinicalRequest(
        String bloodType,
        String allergies,
        String chronicConditions,
        String clinicalNotes
) {}
