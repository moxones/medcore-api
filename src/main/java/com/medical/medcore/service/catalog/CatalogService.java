package com.medical.medcore.service.catalog;

import com.medical.medcore.config.exception.BadRequestException;
import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.request.AppointmentTypeRequest;
import com.medical.medcore.dto.request.DocumentTypeRequest;
import com.medical.medcore.dto.request.SpecialtyRequest;
import com.medical.medcore.dto.response.CatalogItemResponse;
import com.medical.medcore.entity.*;
import com.medical.medcore.repository.*;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Catálogos del dominio.
 *
 * <p>Patrón: el catálogo MAESTRO (specialties, appointment_types, document_types)
 * es global y lo gestiona solo SUPER_ADMIN. Cada clínica (tenant) lo "activa"
 * mediante tablas junction (tenant_*). El combo de disponibles = maestro activo
 * que la clínica todavía NO ha activado.</p>
 *
 * <p>Plans / appointment_status / subscription_status son catálogos de sistema:
 * globales, sin activación por clínica (CRUD solo SUPER_ADMIN, lectura para todos).</p>
 */
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final SpecialtyRepository specialtyRepository;
    private final AppointmentTypeRepository appointmentTypeRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final PlanRepository planRepository;
    private final SubscriptionStatusRepository subscriptionStatusRepository;
    private final AppointmentStatusRepository appointmentStatusRepository;
    private final TenantSpecialtyRepository tenantSpecialtyRepository;
    private final TenantAppointmentTypeRepository tenantAppointmentTypeRepository;
    private final TenantDocumentTypeRepository tenantDocumentTypeRepository;

    // ========================================================
    // MAESTRO - SPECIALTIES (solo SUPER_ADMIN)
    // ========================================================

    public List<Specialty> listMasterSpecialties() {
        return specialtyRepository.findAll();
    }

    public Specialty createSpecialty(SpecialtyRequest req) {
        if (specialtyRepository.existsByCode(req.code())) {
            throw new BadRequestException("Ya existe una especialidad con el código " + req.code());
        }
        Specialty s = Specialty.builder()
                .code(req.code())
                .name(req.name())
                .description(req.description())
                .isActive(req.isActive() == null ? Boolean.TRUE : req.isActive())
                .build();
        return specialtyRepository.save(s);
    }

    public Specialty updateSpecialty(Long id, SpecialtyRequest req) {
        Specialty s = specialtyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Especialidad no encontrada"));
        s.setCode(req.code());
        s.setName(req.name());
        s.setDescription(req.description());
        if (req.isActive() != null) s.setIsActive(req.isActive());
        return specialtyRepository.save(s);
    }

    public void deleteSpecialty(Long id) {
        Specialty s = specialtyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Especialidad no encontrada"));
        specialtyRepository.delete(s);
    }

    // ========================================================
    // MAESTRO - APPOINTMENT TYPES (solo SUPER_ADMIN)
    // ========================================================

    public List<AppointmentType> listMasterAppointmentTypes() {
        return appointmentTypeRepository.findAll();
    }

    public AppointmentType createAppointmentType(AppointmentTypeRequest req) {
        if (appointmentTypeRepository.existsByCode(req.code())) {
            throw new BadRequestException("Ya existe un tipo de cita con el código " + req.code());
        }
        AppointmentType t = AppointmentType.builder()
                .code(req.code())
                .name(req.name())
                .durationMinutes(req.durationMinutes() == null ? 30 : req.durationMinutes())
                .isActive(req.isActive() == null ? Boolean.TRUE : req.isActive())
                .build();
        return appointmentTypeRepository.save(t);
    }

    public AppointmentType updateAppointmentType(Long id, AppointmentTypeRequest req) {
        AppointmentType t = appointmentTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tipo de cita no encontrado"));
        t.setCode(req.code());
        t.setName(req.name());
        if (req.durationMinutes() != null) t.setDurationMinutes(req.durationMinutes());
        if (req.isActive() != null) t.setIsActive(req.isActive());
        return appointmentTypeRepository.save(t);
    }

    public void deleteAppointmentType(Long id) {
        AppointmentType t = appointmentTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tipo de cita no encontrado"));
        appointmentTypeRepository.delete(t);
    }

    // ========================================================
    // MAESTRO - DOCUMENT TYPES (solo SUPER_ADMIN)
    // ========================================================

    public List<DocumentType> listMasterDocumentTypes() {
        return documentTypeRepository.findAll();
    }

    public DocumentType createDocumentType(DocumentTypeRequest req) {
        if (documentTypeRepository.existsByCode(req.code())) {
            throw new BadRequestException("Ya existe un tipo de documento con el código " + req.code());
        }
        DocumentType d = DocumentType.builder()
                .code(req.code())
                .name(req.name())
                .isActive(req.isActive() == null ? Boolean.TRUE : req.isActive())
                .build();
        return documentTypeRepository.save(d);
    }

    public DocumentType updateDocumentType(Long id, DocumentTypeRequest req) {
        DocumentType d = documentTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tipo de documento no encontrado"));
        d.setCode(req.code());
        d.setName(req.name());
        if (req.isActive() != null) d.setIsActive(req.isActive());
        return documentTypeRepository.save(d);
    }

    public void deleteDocumentType(Long id) {
        DocumentType d = documentTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tipo de documento no encontrado"));
        documentTypeRepository.delete(d);
    }

    // ========================================================
    // CLÍNICA - SPECIALTIES (activación / combo)
    // ========================================================

    @Transactional(readOnly = true)
    public List<CatalogItemResponse> listActivatedSpecialties() {
        Long tenantId = TenantContext.requireTenantId();
        return tenantSpecialtyRepository.findByTenantIdFetchSpecialty(tenantId).stream()
                .map(ts -> {
                    Specialty s = ts.getSpecialty();
                    return new CatalogItemResponse(
                            s.getId(), s.getCode(), s.getName(), s.getDescription(),
                            null, s.getIsActive(), true, ts.getIsActive(), ts.getId());
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CatalogItemResponse> listAvailableSpecialties() {
        Long tenantId = TenantContext.requireTenantId();
        Set<Long> activated = tenantSpecialtyRepository.findByTenantIdFetchSpecialty(tenantId).stream()
                .map(ts -> ts.getSpecialty().getId())
                .collect(Collectors.toSet());
        return specialtyRepository.findByIsActiveTrue().stream()
                .filter(s -> !activated.contains(s.getId()))
                .map(s -> new CatalogItemResponse(
                        s.getId(), s.getCode(), s.getName(), s.getDescription(),
                        null, s.getIsActive(), false, null, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public CatalogItemResponse activateSpecialty(Long specialtyId) {
        Long tenantId = TenantContext.requireTenantId();
        Specialty s = specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new NotFoundException("Especialidad no encontrada"));
        if (Boolean.FALSE.equals(s.getIsActive())) {
            throw new BadRequestException("La especialidad está inactiva en el catálogo maestro");
        }
        if (tenantSpecialtyRepository.existsByTenantIdAndSpecialty_Id(tenantId, specialtyId)) {
            throw new BadRequestException("La especialidad ya está activada para esta clínica");
        }
        TenantSpecialty ts = TenantSpecialty.builder()
                .tenantId(tenantId)
                .specialty(s)
                .isActive(true)
                .createdBy(TenantContext.getCurrentUserId())
                .build();
        ts = tenantSpecialtyRepository.save(ts);
        return new CatalogItemResponse(s.getId(), s.getCode(), s.getName(), s.getDescription(),
                null, s.getIsActive(), true, ts.getIsActive(), ts.getId());
    }

    @Transactional
    public void deactivateSpecialty(Long specialtyId) {
        Long tenantId = TenantContext.requireTenantId();
        TenantSpecialty ts = tenantSpecialtyRepository
                .findByTenantIdAndSpecialty_Id(tenantId, specialtyId)
                .orElseThrow(() -> new NotFoundException("La especialidad no está activada para esta clínica"));
        tenantSpecialtyRepository.delete(ts);
    }

    // ========================================================
    // CLÍNICA - APPOINTMENT TYPES (activación / combo)
    // ========================================================

    @Transactional(readOnly = true)
    public List<CatalogItemResponse> listActivatedAppointmentTypes() {
        Long tenantId = TenantContext.requireTenantId();
        return tenantAppointmentTypeRepository.findByTenantIdFetchType(tenantId).stream()
                .map(tat -> {
                    AppointmentType t = tat.getAppointmentType();
                    Integer effective = tat.getDurationMinutes() != null
                            ? tat.getDurationMinutes() : t.getDurationMinutes();
                    return new CatalogItemResponse(
                            t.getId(), t.getCode(), t.getName(), null,
                            effective, t.getIsActive(), true, tat.getIsActive(), tat.getId());
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CatalogItemResponse> listAvailableAppointmentTypes() {
        Long tenantId = TenantContext.requireTenantId();
        Set<Long> activated = tenantAppointmentTypeRepository.findByTenantIdFetchType(tenantId).stream()
                .map(tat -> tat.getAppointmentType().getId())
                .collect(Collectors.toSet());
        return appointmentTypeRepository.findByIsActiveTrue().stream()
                .filter(t -> !activated.contains(t.getId()))
                .map(t -> new CatalogItemResponse(
                        t.getId(), t.getCode(), t.getName(), null,
                        t.getDurationMinutes(), t.getIsActive(), false, null, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public CatalogItemResponse activateAppointmentType(Long appointmentTypeId, Integer durationOverride) {
        Long tenantId = TenantContext.requireTenantId();
        AppointmentType t = appointmentTypeRepository.findById(appointmentTypeId)
                .orElseThrow(() -> new NotFoundException("Tipo de cita no encontrado"));
        if (Boolean.FALSE.equals(t.getIsActive())) {
            throw new BadRequestException("El tipo de cita está inactivo en el catálogo maestro");
        }
        if (tenantAppointmentTypeRepository.existsByTenantIdAndAppointmentType_Id(tenantId, appointmentTypeId)) {
            throw new BadRequestException("El tipo de cita ya está activado para esta clínica");
        }
        TenantAppointmentType tat = TenantAppointmentType.builder()
                .tenantId(tenantId)
                .appointmentType(t)
                .durationMinutes(durationOverride)
                .isActive(true)
                .createdBy(TenantContext.getCurrentUserId())
                .build();
        tat = tenantAppointmentTypeRepository.save(tat);
        Integer effective = tat.getDurationMinutes() != null
                ? tat.getDurationMinutes() : t.getDurationMinutes();
        return new CatalogItemResponse(t.getId(), t.getCode(), t.getName(), null,
                effective, t.getIsActive(), true, tat.getIsActive(), tat.getId());
    }

    @Transactional
    public void deactivateAppointmentType(Long appointmentTypeId) {
        Long tenantId = TenantContext.requireTenantId();
        TenantAppointmentType tat = tenantAppointmentTypeRepository
                .findByTenantIdAndAppointmentType_Id(tenantId, appointmentTypeId)
                .orElseThrow(() -> new NotFoundException("El tipo de cita no está activado para esta clínica"));
        tenantAppointmentTypeRepository.delete(tat);
    }

    // ========================================================
    // CLÍNICA - DOCUMENT TYPES (activación / combo)
    // ========================================================

    @Transactional(readOnly = true)
    public List<CatalogItemResponse> listActivatedDocumentTypes() {
        Long tenantId = TenantContext.requireTenantId();
        return tenantDocumentTypeRepository.findByTenantIdFetchType(tenantId).stream()
                .map(tdt -> {
                    DocumentType d = tdt.getDocumentType();
                    return new CatalogItemResponse(
                            d.getId(), d.getCode(), d.getName(), null,
                            null, d.getIsActive(), true, tdt.getIsActive(), tdt.getId());
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CatalogItemResponse> listAvailableDocumentTypes() {
        Long tenantId = TenantContext.requireTenantId();
        Set<Long> activated = tenantDocumentTypeRepository.findByTenantIdFetchType(tenantId).stream()
                .map(tdt -> tdt.getDocumentType().getId())
                .collect(Collectors.toSet());
        return documentTypeRepository.findByIsActiveTrue().stream()
                .filter(d -> !activated.contains(d.getId()))
                .map(d -> new CatalogItemResponse(
                        d.getId(), d.getCode(), d.getName(), null,
                        null, d.getIsActive(), false, null, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public CatalogItemResponse activateDocumentType(Long documentTypeId) {
        Long tenantId = TenantContext.requireTenantId();
        DocumentType d = documentTypeRepository.findById(documentTypeId)
                .orElseThrow(() -> new NotFoundException("Tipo de documento no encontrado"));
        if (Boolean.FALSE.equals(d.getIsActive())) {
            throw new BadRequestException("El tipo de documento está inactivo en el catálogo maestro");
        }
        if (tenantDocumentTypeRepository.existsByTenantIdAndDocumentType_Id(tenantId, documentTypeId)) {
            throw new BadRequestException("El tipo de documento ya está activado para esta clínica");
        }
        TenantDocumentType tdt = TenantDocumentType.builder()
                .tenantId(tenantId)
                .documentType(d)
                .isActive(true)
                .createdBy(TenantContext.getCurrentUserId())
                .build();
        tdt = tenantDocumentTypeRepository.save(tdt);
        return new CatalogItemResponse(d.getId(), d.getCode(), d.getName(), null,
                null, d.getIsActive(), true, tdt.getIsActive(), tdt.getId());
    }

    @Transactional
    public void deactivateDocumentType(Long documentTypeId) {
        Long tenantId = TenantContext.requireTenantId();
        TenantDocumentType tdt = tenantDocumentTypeRepository
                .findByTenantIdAndDocumentType_Id(tenantId, documentTypeId)
                .orElseThrow(() -> new NotFoundException("El tipo de documento no está activado para esta clínica"));
        tenantDocumentTypeRepository.delete(tdt);
    }

    // ========================================================
    // SISTEMA - PLANS (global, sin activación por clínica)
    // ========================================================

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

    // ========================================================
    // SISTEMA - SUBSCRIPTION STATUS
    // ========================================================

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

    // ========================================================
    // SISTEMA - APPOINTMENT STATUS
    // ========================================================

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
        if (status.getIsActive() != null) existing.setIsActive(status.getIsActive());
        return appointmentStatusRepository.save(existing);
    }

    public void deleteAppointmentStatus(Long id) {
        appointmentStatusRepository.delete(getAppointmentStatusById(id));
    }
}
