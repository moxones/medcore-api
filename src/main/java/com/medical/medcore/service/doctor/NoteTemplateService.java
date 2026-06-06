package com.medical.medcore.service.doctor;

import com.medical.medcore.config.exception.NotFoundException;
import com.medical.medcore.dto.request.SaveNoteTemplateRequest;
import com.medical.medcore.dto.response.NoteTemplateResponse;
import com.medical.medcore.entity.Doctor;
import com.medical.medcore.entity.NoteTemplate;
import com.medical.medcore.repository.NoteTemplateRepository;
import com.medical.medcore.util.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteTemplateService {

    private final NoteTemplateRepository noteTemplateRepository;
    private final DoctorService doctorService;

    @Transactional(readOnly = true)
    public List<NoteTemplateResponse> list() {
        Long tenantId = TenantContext.requireTenantId();
        Doctor doctor = doctorService.findMe();
        return noteTemplateRepository
                .findByTenantIdAndDoctorIdOrderByUpdatedAtDescIdDesc(tenantId, doctor.getId())
                .stream().map(this::map).toList();
    }

    @Transactional
    public NoteTemplateResponse create(SaveNoteTemplateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.getCurrentUserId();
        Doctor doctor = doctorService.findMe();

        NoteTemplate template = NoteTemplate.builder()
                .tenantId(tenantId)
                .doctorId(doctor.getId())
                .name(request.name())
                .chiefComplaint(request.chiefComplaint())
                .presentIllness(request.presentIllness())
                .physicalExamination(request.physicalExamination())
                .assessment(request.assessment())
                .plan(request.plan())
                .treatment(request.treatment())
                .notes(request.notes())
                .usageCount(0L)
                .createdBy(userId)
                .build();
        return map(noteTemplateRepository.save(template));
    }

    @Transactional
    public NoteTemplateResponse update(Long id, SaveNoteTemplateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = TenantContext.getCurrentUserId();
        Doctor doctor = doctorService.findMe();

        NoteTemplate template = noteTemplateRepository
                .findByIdAndTenantIdAndDoctorId(id, tenantId, doctor.getId())
                .orElseThrow(() -> new NotFoundException("Plantilla no encontrada"));

        template.setName(request.name());
        template.setChiefComplaint(request.chiefComplaint());
        template.setPresentIllness(request.presentIllness());
        template.setPhysicalExamination(request.physicalExamination());
        template.setAssessment(request.assessment());
        template.setPlan(request.plan());
        template.setTreatment(request.treatment());
        template.setNotes(request.notes());
        template.setUpdatedBy(userId);
        return map(noteTemplateRepository.save(template));
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Doctor doctor = doctorService.findMe();
        NoteTemplate template = noteTemplateRepository
                .findByIdAndTenantIdAndDoctorId(id, tenantId, doctor.getId())
                .orElseThrow(() -> new NotFoundException("Plantilla no encontrada"));
        noteTemplateRepository.delete(template);
    }

    private NoteTemplateResponse map(NoteTemplate t) {
        return new NoteTemplateResponse(
                t.getId(),
                t.getName(),
                t.getSpecialtyName(),
                t.getChiefComplaint(),
                t.getPresentIllness(),
                t.getPhysicalExamination(),
                t.getAssessment(),
                t.getPlan(),
                t.getTreatment(),
                t.getNotes(),
                t.getUsageCount() != null ? t.getUsageCount() : 0L,
                t.getUpdatedAt()
        );
    }
}
