package com.medical.medcore.doctor.controller;

import com.medical.medcore.doctor.dto.SaveNoteTemplateRequest;
import com.medical.medcore.doctor.dto.DoctorOrderResponse;
import com.medical.medcore.doctor.dto.DoctorPatientResponse;
import com.medical.medcore.doctor.dto.DoctorProfileResponse;
import com.medical.medcore.doctor.dto.NoteTemplateResponse;
import com.medical.medcore.doctor.dto.PrescriptionDocumentResponse;
import com.medical.medcore.shared.security.annotation.RequireDoctor;
import com.medical.medcore.doctor.service.DoctorPortalService;
import com.medical.medcore.doctor.service.NoteTemplateService;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.api.PageableResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/doctors/me")
@RequiredArgsConstructor
@RequireDoctor
public class DoctorPortalController {

    private final DoctorPortalService doctorPortalService;
    private final NoteTemplateService noteTemplateService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<DoctorProfileResponse>> profile() {
        return ResponseEntity.ok(new ApiResponse<>(true,
                doctorPortalService.getProfile(), "Perfil del médico"));
    }

    @GetMapping("/patients")
    public ResponseEntity<ApiResponse<PageableResponse<DoctorPatientResponse>>> patients(
            @RequestParam(required = false, name = "q") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                doctorPortalService.getPatients(q, page, size), "Mis pacientes"));
    }

    @GetMapping("/prescriptions")
    public ResponseEntity<ApiResponse<PageableResponse<PrescriptionDocumentResponse>>> prescriptions(
            @RequestParam(required = false, name = "q") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                doctorPortalService.getPrescriptions(q, page, size), "Recetas emitidas"));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<DoctorOrderResponse>>> orders(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                doctorPortalService.getOrders(status), "Órdenes y exámenes"));
    }

    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<NoteTemplateResponse>>> listTemplates() {
        return ResponseEntity.ok(new ApiResponse<>(true,
                noteTemplateService.list(), "Plantillas de nota clínica"));
    }

    @PostMapping("/templates")
    public ResponseEntity<ApiResponse<NoteTemplateResponse>> createTemplate(
            @Valid @RequestBody SaveNoteTemplateRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                noteTemplateService.create(request), "Plantilla creada"));
    }

    @PutMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<NoteTemplateResponse>> updateTemplate(
            @PathVariable Long id, @Valid @RequestBody SaveNoteTemplateRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true,
                noteTemplateService.update(id, request), "Plantilla actualizada"));
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        noteTemplateService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Plantilla eliminada"));
    }
}
