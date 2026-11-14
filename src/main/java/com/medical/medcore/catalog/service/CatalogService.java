package com.medical.medcore.catalog.service;

import com.medical.medcore.catalog.domain.AppointmentStatus;
import com.medical.medcore.catalog.domain.AppointmentType;
import com.medical.medcore.catalog.domain.DocumentType;
import com.medical.medcore.catalog.domain.Specialty;
import com.medical.medcore.catalog.domain.TenantAppointmentType;
import com.medical.medcore.catalog.domain.TenantDocumentType;
import com.medical.medcore.catalog.domain.TenantSpecialty;
import com.medical.medcore.catalog.repository.AppointmentStatusRepository;
import com.medical.medcore.catalog.repository.AppointmentTypeRepository;
import com.medical.medcore.catalog.repository.DocumentTypeRepository;
import com.medical.medcore.catalog.repository.SpecialtyRepository;
import com.medical.medcore.catalog.repository.TenantAppointmentTypeRepository;
import com.medical.medcore.catalog.repository.TenantDocumentTypeRepository;
import com.medical.medcore.catalog.repository.TenantSpecialtyRepository;
import com.medical.medcore.platform.domain.Plan;
import com.medical.medcore.platform.domain.SubscriptionStatus;
import com.medical.medcore.platform.repository.PlanRepository;
import com.medical.medcore.platform.repository.SubscriptionStatusRepository;

import com.medical.medcore.shared.exception.BadRequestException;
import com.medical.medcore.shared.exception.NotFoundException;
import com.medical.medcore.catalog.dto.AppointmentTypeRequest;
import com.medical.medcore.catalog.dto.DocumentTypeRequest;
import com.medical.medcore.catalog.dto.SpecialtyRequest;
import com.medical.medcore.catalog.dto.CatalogItemResponse;
import com.medical.medcore.shared.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
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
                            effective, t.getIsActive(), true, tat.getIsActive(), tat.getId(), tat.getPrice());
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
    public CatalogItemResponse activateAppointmentType(Long appointmentTypeId, Integer durationOverride,
                                                       java.math.BigDecimal price) {
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
                .price(price)
                .isActive(true)
                .createdBy(TenantContext.getCurrentUserId())
                .build();
        tat = tenantAppointmentTypeRepository.save(tat);
        Integer effective = tat.getDurationMinutes() != null
                ? tat.getDurationMinutes() : t.getDurationMinutes();
        return new CatalogItemResponse(t.getId(), t.getCode(), t.getName(), null,
                effective, t.getIsActive(), true, tat.getIsActive(), tat.getId(), tat.getPrice());
    }

    @Transactional
    public CatalogItemResponse updateAppointmentType(Long appointmentTypeId, Integer durationOverride,
                                                     java.math.BigDecimal price) {
        Long tenantId = TenantContext.requireTenantId();
        TenantAppointmentType tat = tenantAppointmentTypeRepository
                .findByTenantIdAndAppointmentType_Id(tenantId, appointmentTypeId)
                .orElseThrow(() -> new NotFoundException("El tipo de cita no está activado para esta clínica"));
        tat.setDurationMinutes(durationOverride);
        tat.setPrice(price);
        tat = tenantAppointmentTypeRepository.save(tat);
        AppointmentType t = tat.getAppointmentType();
        Integer effective = tat.getDurationMinutes() != null ? tat.getDurationMinutes() : t.getDurationMinutes();
        return new CatalogItemResponse(t.getId(), t.getCode(), t.getName(), null,
                effective, t.getIsActive(), true, tat.getIsActive(), tat.getId(), tat.getPrice());
    }

    @Transactional
    public void deactivateAppointmentType(Long appointmentTypeId) {
        Long tenantId = TenantContext.requireTenantId();
        TenantAppointmentType tat = tenantAppointmentTypeRepository
                .findByTenantIdAndAppointmentType_Id(tenantId, appointmentTypeId)
                .orElseThrow(() -> new NotFoundException("El tipo de cita no está activado para esta clínica"));
        tenantAppointmentTypeRepository.delete(tat);
    }

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
}
