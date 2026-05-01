package com.medical.medcore.service.catalog;

import com.medical.medcore.entity.*;
import com.medical.medcore.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final SpecialtyRepository specialtyRepository;
    private final PlanRepository planRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final SubscriptionStatusRepository subscriptionStatusRepository;
    private final AppointmentStatusRepository appointmentStatusRepository;
    private final AppointmentTypeRepository appointmentTypeRepository;

    public List<Specialty> getSpecialties() {
        return specialtyRepository.findByIsActiveTrue();
    }

    public Specialty getSpecialtyById(Long id) {
        return specialtyRepository.findById(id).orElseThrow();
    }

    public Specialty createSpecialty(Specialty specialty) {
        return specialtyRepository.save(specialty);
    }

    public Specialty updateSpecialty(Long id, Specialty specialty) {
        Specialty existing = getSpecialtyById(id);
        existing.setName(specialty.getName());
        existing.setCode(specialty.getCode());
        existing.setIsActive(specialty.getIsActive());
        return specialtyRepository.save(existing);
    }

    public void deleteSpecialty(Long id) {
        specialtyRepository.deleteById(id);
    }

    public List<Plan> getPlans() {
        return planRepository.findByIsActiveTrue();
    }

    public Plan getPlanById(Long id) {
        return planRepository.findById(id).orElseThrow();
    }

    public Plan createPlan(Plan plan) {
        return planRepository.save(plan);
    }

    public Plan updatePlan(Long id, Plan plan) {
        Plan existing = getPlanById(id);
        existing.setName(plan.getName());
        existing.setCode(plan.getCode());
        existing.setPrice(plan.getPrice());
        existing.setMaxUsers(plan.getMaxUsers());
        existing.setMaxBranches(plan.getMaxBranches());
        existing.setIsActive(plan.getIsActive());
        return planRepository.save(existing);
    }

    public void deletePlan(Long id) {
        planRepository.deleteById(id);
    }

    public List<DocumentType> getDocumentTypes() {
        return documentTypeRepository.findAll();
    }

    public DocumentType getDocumentTypeById(Long id) {
        return documentTypeRepository.findById(id).orElseThrow();
    }

    public DocumentType createDocumentType(DocumentType docType) {
        return documentTypeRepository.save(docType);
    }

    public DocumentType updateDocumentType(Long id, DocumentType docType) {
        DocumentType existing = getDocumentTypeById(id);
        existing.setCode(docType.getCode());
        existing.setName(docType.getName());
        return documentTypeRepository.save(existing);
    }

    public void deleteDocumentType(Long id) {
        documentTypeRepository.deleteById(id);
    }

    public List<SubscriptionStatus> getSubscriptionStatuses() {
        return subscriptionStatusRepository.findAll();
    }

    public SubscriptionStatus getSubscriptionStatusById(Long id) {
        return subscriptionStatusRepository.findById(id).orElseThrow();
    }

    public SubscriptionStatus createSubscriptionStatus(SubscriptionStatus status) {
        return subscriptionStatusRepository.save(status);
    }

    public SubscriptionStatus updateSubscriptionStatus(Long id, SubscriptionStatus status) {
        SubscriptionStatus existing = getSubscriptionStatusById(id);
        existing.setCode(status.getCode());
        return subscriptionStatusRepository.save(existing);
    }

    public void deleteSubscriptionStatus(Long id) {
        subscriptionStatusRepository.deleteById(id);
    }

    public List<AppointmentStatus> getAppointmentStatuses() {
        return appointmentStatusRepository.findAll();
    }

    public AppointmentStatus getAppointmentStatusById(Long id) {
        return appointmentStatusRepository.findById(id).orElseThrow();
    }

    public AppointmentStatus createAppointmentStatus(AppointmentStatus status) {
        return appointmentStatusRepository.save(status);
    }

    public AppointmentStatus updateAppointmentStatus(Long id, AppointmentStatus status) {
        AppointmentStatus existing = getAppointmentStatusById(id);
        existing.setCode(status.getCode());
        existing.setName(status.getName());
        return appointmentStatusRepository.save(existing);
    }

    public void deleteAppointmentStatus(Long id) {
        appointmentStatusRepository.deleteById(id);
    }

    public List<AppointmentType> getAppointmentTypes() {
        return appointmentTypeRepository.findAll();
    }

    public AppointmentType getAppointmentTypeById(Long id) {
        return appointmentTypeRepository.findById(id).orElseThrow();
    }

    public AppointmentType createAppointmentType(AppointmentType type) {
        return appointmentTypeRepository.save(type);
    }

    public AppointmentType updateAppointmentType(Long id, AppointmentType type) {
        AppointmentType existing = getAppointmentTypeById(id);
        existing.setCode(type.getCode());
        existing.setName(type.getName());
        existing.setDurationMinutes(type.getDurationMinutes());
        existing.setIsActive(type.getIsActive());
        return appointmentTypeRepository.save(existing);
    }

    public void deleteAppointmentType(Long id) {
        appointmentTypeRepository.deleteById(id);
    }
}
