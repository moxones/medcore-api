package com.medical.medcore.service.patienthistory.impl;

import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.request.PatientAllergyRequest;
import com.medical.medcore.dto.request.PatientConditionRequest;
import com.medical.medcore.dto.request.PatientFamilyHistoryRequest;
import com.medical.medcore.dto.request.PatientHabitRequest;
import com.medical.medcore.dto.request.PatientSurgicalHistoryRequest;
import com.medical.medcore.dto.response.PatientClinicalHistoryResponse;
import com.medical.medcore.dto.response.PatientClinicalHistoryResponse.*;
import com.medical.medcore.entity.*;
import com.medical.medcore.repository.*;
import com.medical.medcore.service.patienthistory.PatientHistoryService;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientHistoryServiceImpl implements PatientHistoryService {

    private final PatientRepository patientRepository;
    private final PatientAllergyRepository allergyRepository;
    private final PatientConditionRepository conditionRepository;
    private final PatientFamilyHistoryRepository familyHistoryRepository;
    private final PatientSurgicalHistoryRepository surgicalHistoryRepository;
    private final PatientHabitRepository habitRepository;
    private final Cie10CodeRepository cie10CodeRepository;

    @Override
    @Transactional(readOnly = true)
    public PatientClinicalHistoryResponse getHistory(Long patientId) {
        Patient patient = requirePatient(patientId);

        List<PatientCondition> conditions = conditionRepository.findByPatientIdOrderByIdDesc(patientId);
        Map<Long, String> cie10Codes = resolveCie10Codes(conditions);

        return new PatientClinicalHistoryResponse(
                patient.getId(),
                patient.getBloodType(),
                allergyRepository.findByPatientIdOrderByIdDesc(patientId).stream()
                        .map(this::mapAllergy).toList(),
                conditions.stream().map(c -> mapCondition(c, cie10Codes)).toList(),
                familyHistoryRepository.findByPatientIdOrderByIdDesc(patientId).stream()
                        .map(this::mapFamily).toList(),
                surgicalHistoryRepository.findByPatientIdOrderByIdDesc(patientId).stream()
                        .map(this::mapSurgical).toList(),
                habitRepository.findByPatientIdOrderByIdDesc(patientId).stream()
                        .map(this::mapHabit).toList()
        );
    }

    @Override
    @Transactional
    public AllergyItem addAllergy(Long patientId, PatientAllergyRequest request) {
        requirePatient(patientId);
        PatientAllergy entity = PatientAllergy.builder()
                .patientId(patientId)
                .allergen(request.allergen())
                .reaction(request.reaction())
                .severity(request.severity())
                .isActive(request.isActive() != null ? request.isActive() : true)
                .createdBy(TenantContext.getCurrentUserId())
                .build();
        return mapAllergy(allergyRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteAllergy(Long patientId, Long id) {
        requirePatient(patientId);
        PatientAllergy entity = allergyRepository.findById(id)
                .filter(a -> a.getPatientId().equals(patientId))
                .orElseThrow(() -> new NotFoundException("Alergia no encontrada"));
        allergyRepository.delete(entity);
    }

    @Override
    @Transactional
    public ConditionItem addCondition(Long patientId, PatientConditionRequest request) {
        requirePatient(patientId);
        PatientCondition entity = PatientCondition.builder()
                .patientId(patientId)
                .cie10Id(request.cie10Id())
                .description(request.description())
                .status(request.status() != null ? request.status() : "ACTIVE")
                .diagnosedAt(request.diagnosedAt())
                .createdBy(TenantContext.getCurrentUserId())
                .build();
        entity = conditionRepository.save(entity);
        return mapCondition(entity, resolveCie10Codes(List.of(entity)));
    }

    @Override
    @Transactional
    public void deleteCondition(Long patientId, Long id) {
        requirePatient(patientId);
        PatientCondition entity = conditionRepository.findById(id)
                .filter(c -> c.getPatientId().equals(patientId))
                .orElseThrow(() -> new NotFoundException("Condición no encontrada"));
        conditionRepository.delete(entity);
    }

    @Override
    @Transactional
    public FamilyHistoryItem addFamilyHistory(Long patientId, PatientFamilyHistoryRequest request) {
        requirePatient(patientId);
        PatientFamilyHistory entity = PatientFamilyHistory.builder()
                .patientId(patientId)
                .relationship(request.relationship())
                .condition(request.condition())
                .notes(request.notes())
                .createdBy(TenantContext.getCurrentUserId())
                .build();
        return mapFamily(familyHistoryRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteFamilyHistory(Long patientId, Long id) {
        requirePatient(patientId);
        PatientFamilyHistory entity = familyHistoryRepository.findById(id)
                .filter(f -> f.getPatientId().equals(patientId))
                .orElseThrow(() -> new NotFoundException("Antecedente familiar no encontrado"));
        familyHistoryRepository.delete(entity);
    }

    @Override
    @Transactional
    public SurgicalHistoryItem addSurgicalHistory(Long patientId, PatientSurgicalHistoryRequest request) {
        requirePatient(patientId);
        PatientSurgicalHistory entity = PatientSurgicalHistory.builder()
                .patientId(patientId)
                .procedure(request.procedure())
                .performedOn(request.performedOn())
                .notes(request.notes())
                .createdBy(TenantContext.getCurrentUserId())
                .build();
        return mapSurgical(surgicalHistoryRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteSurgicalHistory(Long patientId, Long id) {
        requirePatient(patientId);
        PatientSurgicalHistory entity = surgicalHistoryRepository.findById(id)
                .filter(s -> s.getPatientId().equals(patientId))
                .orElseThrow(() -> new NotFoundException("Antecedente quirúrgico no encontrado"));
        surgicalHistoryRepository.delete(entity);
    }

    @Override
    @Transactional
    public HabitItem addHabit(Long patientId, PatientHabitRequest request) {
        requirePatient(patientId);
        PatientHabit entity = PatientHabit.builder()
                .patientId(patientId)
                .habitType(request.habitType())
                .detail(request.detail())
                .status(request.status() != null ? request.status() : "ACTIVE")
                .createdBy(TenantContext.getCurrentUserId())
                .build();
        return mapHabit(habitRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteHabit(Long patientId, Long id) {
        requirePatient(patientId);
        PatientHabit entity = habitRepository.findById(id)
                .filter(h -> h.getPatientId().equals(patientId))
                .orElseThrow(() -> new NotFoundException("Hábito no encontrado"));
        habitRepository.delete(entity);
    }

    private Patient requirePatient(Long patientId) {
        Long tenantId = TenantContext.requireTenantId();
        return patientRepository.findByIdAndTenantId(patientId, tenantId)
                .orElseThrow(() -> new NotFoundException("Paciente no encontrado"));
    }

    private Map<Long, String> resolveCie10Codes(List<PatientCondition> conditions) {
        List<Long> ids = conditions.stream()
                .map(PatientCondition::getCie10Id)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) return Map.of();
        return cie10CodeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Cie10Code::getId, Cie10Code::getCode));
    }

    private AllergyItem mapAllergy(PatientAllergy a) {
        return new AllergyItem(a.getId(), a.getAllergen(), a.getReaction(), a.getSeverity(), a.getIsActive());
    }

    private ConditionItem mapCondition(PatientCondition c, Map<Long, String> cie10Codes) {
        return new ConditionItem(c.getId(), c.getCie10Id(),
                c.getCie10Id() != null ? cie10Codes.get(c.getCie10Id()) : null,
                c.getDescription(), c.getStatus(), c.getDiagnosedAt());
    }

    private FamilyHistoryItem mapFamily(PatientFamilyHistory f) {
        return new FamilyHistoryItem(f.getId(), f.getRelationship(), f.getCondition(), f.getNotes());
    }

    private SurgicalHistoryItem mapSurgical(PatientSurgicalHistory s) {
        return new SurgicalHistoryItem(s.getId(), s.getProcedure(), s.getPerformedOn(), s.getNotes());
    }

    private HabitItem mapHabit(PatientHabit h) {
        return new HabitItem(h.getId(), h.getHabitType(), h.getDetail(), h.getStatus());
    }
}
