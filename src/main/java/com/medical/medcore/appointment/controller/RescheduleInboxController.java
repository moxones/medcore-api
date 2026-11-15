package com.medical.medcore.appointment.controller;

import com.medical.medcore.appointment.dto.AppointmentResponse;
import com.medical.medcore.appointment.dto.RescheduleInboxSummaryResponse;
import com.medical.medcore.appointment.dto.RescheduleSuggestionsResponse;
import com.medical.medcore.appointment.service.RescheduleInboxService;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.security.annotation.RequireReception;
import com.medical.medcore.shared.security.annotation.RequireStaff;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RescheduleInboxController {

    private final RescheduleInboxService service;

    @RequireStaff
    @GetMapping("/reschedule-inbox")
    public ApiResponse<List<AppointmentResponse>> list(@RequestParam(required = false) Long branchId,
                                                       @RequestParam(required = false) Long doctorId) {
        return ApiResponse.ok(service.list(branchId, doctorId), "Citas por reprogramar");
    }

    @RequireStaff
    @GetMapping("/reschedule-inbox/summary")
    public ApiResponse<RescheduleInboxSummaryResponse> summary(@RequestParam(required = false) Long branchId) {
        return ApiResponse.ok(service.summary(branchId), "Resumen de citas por reprogramar");
    }

    @RequireStaff
    @GetMapping("/appointments/{id}/reschedule-suggestions")
    public ApiResponse<RescheduleSuggestionsResponse> suggestions(@PathVariable Long id) {
        return ApiResponse.ok(service.suggestions(id), "Horarios sugeridos");
    }

    @RequireReception
    @PostMapping("/appointments/{id}/reschedule-flag/dismiss")
    public ApiResponse<AppointmentResponse> dismiss(@PathVariable Long id) {
        return ApiResponse.ok(service.dismiss(id), "La cita se mantiene");
    }
}
