package com.medical.medcore.repository.impl;

import com.medical.medcore.entity.Patient;
import com.medical.medcore.repository.PatientSearchRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PatientSearchRepositoryImpl implements PatientSearchRepository {

    @PersistenceContext
    private final EntityManager em;

    @Override
    public List<Patient> searchByDocument(Long tenantId, String document) {

        String jpql = """
                SELECT p FROM Patient p
                JOIN p.person per
                JOIN PersonDocument pd ON pd.person.id = per.id
                WHERE p.tenantId = :tenantId
                AND pd.documentNumber = :document
                """;

        return em.createQuery(jpql, Patient.class)
                .setParameter("tenantId", tenantId)
                .setParameter("document", document)
                .getResultList();
    }

    @Override
    public List<Patient> searchByText(Long tenantId, String term) {

        String jpql = """
                SELECT p FROM Patient p
                JOIN p.person per
                LEFT JOIN User u ON u.person.id = per.id AND u.tenantId = p.tenantId
                WHERE p.tenantId = :tenantId
                AND (
                    LOWER(per.firstName) LIKE :term OR
                    LOWER(per.lastName) LIKE :term OR
                    LOWER(per.contactEmail) LIKE :term OR
                    LOWER(u.email) LIKE :term
                )
                """;

        return em.createQuery(jpql, Patient.class)
                .setParameter("tenantId", tenantId)
                .setParameter("term", "%" + term.toLowerCase() + "%")
                .getResultList();
    }
}