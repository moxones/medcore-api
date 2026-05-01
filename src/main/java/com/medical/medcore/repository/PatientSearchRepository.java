package com.medical.medcore.repository;

import com.medical.medcore.entity.Patient;

import java.util.List;

public interface PatientSearchRepository {

    List<Patient> searchByDocument(Long tenantId, String document);

    List<Patient> searchByText(Long tenantId, String term);
}