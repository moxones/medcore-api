package com.medical.medcore.service.catalog;

import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.entity.*;
import com.medical.medcore.repository.*;
import com.medical.medcore.util.TenantContext;
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
        return specialtyRepository.findByTenantIdAndIsActiveTrue(TenantContext.requireTenantId());
    }

    public Specialty getSpecialtyById(Long id) {
        return specialtyRepository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException("Especialidad no encontrada"));
    }

    public Specialty createSpecialty(Specialty specialty) {
        specialty.setTenantId(TenantContext.requireTenantId());
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
        Specialty existing = getSpecialtyById(id);
        specialtyRepository.delete(existing);
    }

    public List<Plan> getPlans() {
        return planRepository.findByIsActiveTrue();
    }

    public Plan getPlanById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Plan no encontrado"));
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
        planRepository.delete(getPlanById(id));
    }

    public List<DocumentType> getDocumentTypes() {
        return documentTypeRepository.findAll();
    }

    public DocumentType getDocumentTypeById(Long id) {
        return documentTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tipo de documento no encontrado"));
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
        documentTypeRepository.delete(getDocumentTypeById(id));
    }

    public List<SubscriptionStatus> getSubscriptionStatuses() {
        return subscriptionStatusRepository.findAll();
    }

    public SubscriptionStatus getSubscriptionStatusById(Long id) {
        return subscriptionStatusRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Estado de suscripción no encontrado"));
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
        subscriptionStatusRepository.delete(getSubscriptionStatusById(id));
    }

    public List<AppointmentStatus> getAppointmentStatuses() {
        return appointmentStatusRepository.findAll();
    }

    public AppointmentStatus getAppointmentStatusById(Long id) {
        return appointmentStatusRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Estado de cita no encontrado"));
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
        appointmentStatusRepository.delete(getAppointmentStatusById(id));
    }

    public List<AppointmentType> getAppointmentTypes() {
        return appointmentTypeRepository.findAllByTenantId(TenantContext.requireTenantId());
    }

    public AppointmentType getAppointmentTypeById(Long id) {
        return appointmentTypeRepository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> new NotFoundException("Tipo de cita no encontrado"));
    }

    public AppointmentType createAppointmentType(AppointmentType type) {
        type.setTenantId(TenantContext.requireTenantId());
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
        AppointmentType existing = getAppointmentTypeById(id);
        appointmentTypeRepository.delete(existing);
    }

    public List<Specialty> getSpecialtiesForSuperAdmin(Long tenantId) {
        if (tenantId == null) {
            return specialtyRepository.findAll();
        }
        return specialtyRepository.findByTenantIdAndIsActiveTrue(tenantId);
    }

    public Specialty createSpecialtyForTenant(Long tenantId, Specialty specialty) {
        specialty.setTenantId(tenantId);
        return specialtyRepository.save(specialty);
    }

    public Specialty updateSpecialtyForSuperAdmin(Long id, Specialty specialty) {
        Specialty existing = specialtyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Especialidad no encontrada"));
        existing.setName(specialty.getName());
        existing.setCode(specialty.getCode());
        existing.setIsActive(specialty.getIsActive());
        return specialtyRepository.save(existing);
    }

    public void deleteSpecialtyForSuperAdmin(Long id) {
        Specialty existing = specialtyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Especialidad no encontrada"));
        specialtyRepository.delete(existing);
    }

    public List<AppointmentType> getAppointmentTypesForSuperAdmin(Long tenantId) {
        if (tenantId == null) {
            return appointmentTypeRepository.findAll();
        }
        return appointmentTypeRepository.findAllByTenantId(tenantId);
    }

    public AppointmentType createAppointmentTypeForTenant(Long tenantId, AppointmentType type) {
        type.setTenantId(tenantId);
        return appointmentTypeRepository.save(type);
    }

    public AppointmentType updateAppointmentTypeForSuperAdmin(Long id, AppointmentType type) {
        AppointmentType existing = appointmentTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tipo de cita no encontrado"));
        existing.setCode(type.getCode());
        existing.setName(type.getName());
        existing.setDurationMinutes(type.getDurationMinutes());
        existing.setIsActive(type.getIsActive());
        return appointmentTypeRepository.save(existing);
    }

    public void deleteAppointmentTypeForSuperAdmin(Long id) {
        AppointmentType existing = appointmentTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tipo de cita no encontrado"));
        appointmentTypeRepository.delete(existing);
    }
}
