package com.medical.medcore.schedule.controller;

import com.medical.medcore.schedule.dto.DoctorCalendarResponse;
import com.medical.medcore.schedule.dto.ExceptionOutcomeResponse;
import com.medical.medcore.schedule.dto.ImpactPreviewResponse;
import com.medical.medcore.schedule.dto.ResolutionRequest;
import com.medical.medcore.schedule.dto.ReviewRequest;
import com.medical.medcore.schedule.dto.ScheduleExceptionRequest;
import com.medical.medcore.schedule.dto.ScheduleExceptionResponse;
import com.medical.medcore.schedule.service.ScheduleExceptionService;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.security.annotation.RequireAdminOrSuperAdmin;
import com.medical.medcore.shared.security.annotation.RequireStaff;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/schedule-exceptions")
@RequiredArgsConstructor
public class ScheduleExceptionController {

    private final ScheduleExceptionService service;

    @RequireStaff
    @GetMapping
    public ApiResponse<List<ScheduleExceptionResponse>> list(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.list(doctorId, from, to, status), "Excepciones de horario");
    }

    @RequireAdminOrSuperAdmin
    @GetMapping("/pending")
    public ApiResponse<List<ScheduleExceptionResponse>> pending() {
        return ApiResponse.ok(service.pending(), "Solicitudes pendientes");
    }

    @RequireStaff
    @GetMapping("/calendar")
    public ApiResponse<DoctorCalendarResponse> calendar(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(service.calendar(doctorId, from, to), "Calendario del profesional");
    }

    @RequireStaff
    @PostMapping("/preview")
    public ApiResponse<ImpactPreviewResponse> preview(@Valid @RequestBody ScheduleExceptionRequest request) {
        return ApiResponse.ok(service.preview(request), "Impacto del cambio");
    }

    @RequireStaff
    @PostMapping
    public ApiResponse<ExceptionOutcomeResponse> create(@Valid @RequestBody ScheduleExceptionRequest request) {
        ExceptionOutcomeResponse outcome = service.create(request);
        String message = "PENDING".equals(outcome.exception().status())
                ? "Solicitud enviada a la administración"
                : "Cambio de horario registrado";
        return ApiResponse.ok(outcome, message);
    }

    @RequireStaff
    @GetMapping("/{id}/cancel-preview")
    public ApiResponse<ImpactPreviewResponse> cancelPreview(@PathVariable Long id) {
        return ApiResponse.ok(service.cancelPreview(id), "Impacto de anular");
    }

    @RequireStaff
    @PostMapping("/{id}/cancel")
    public ApiResponse<ExceptionOutcomeResponse> cancel(@PathVariable Long id,
                                                        @Valid @RequestBody(required = false) ResolutionRequest request) {
        return ApiResponse.ok(service.cancel(id, request), "Cambio de horario anulado");
    }

    @RequireAdminOrSuperAdmin
    @PostMapping("/{id}/approve")
    public ApiResponse<ExceptionOutcomeResponse> approve(@PathVariable Long id,
                                                         @Valid @RequestBody(required = false) ReviewRequest request) {
        return ApiResponse.ok(service.approve(id, request != null ? request.note() : null), "Solicitud aprobada");
    }

    @RequireAdminOrSuperAdmin
    @PostMapping("/{id}/reject")
    public ApiResponse<ExceptionOutcomeResponse> reject(@PathVariable Long id,
                                                        @Valid @RequestBody(required = false) ReviewRequest request) {
        return ApiResponse.ok(service.reject(id, request != null ? request.note() : null), "Solicitud rechazada");
    }
}
