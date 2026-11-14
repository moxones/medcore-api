package com.medical.medcore.clinical.dto;

public record UpdatePatientClinicalRequest(
        String bloodType,
        String allergies,
        String chronicConditions,
        String clinicalNotes
) {}
