package com.medical.medcore.controller;

import com.medical.medcore.dto.request.CancelAppointmentRequest;
import com.medical.medcore.dto.request.CreateAppointmentRequest;
import com.medical.medcore.dto.request.RescheduleAppointmentRequest;
import com.medical.medcore.dto.request.UpdateAppointmentFlowRequest;
import com.medical.medcore.dto.response.AppointmentResponse;
import com.medical.medcore.dto.response.TimeSlotResponse;
import com.medical.medcore.service.appointment.AppointmentService;
import com.medical.medcore.types.ApiResponse;
import com.medical.medcore.types.PageableResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> create(
            @Valid @RequestBody CreateAppointmentRequest request) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, appointmentService.create(request), "Cita programada con éxito")
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageableResponse<AppointmentResponse>>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long statusId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
            
        return ResponseEntity.ok(
                new ApiResponse<>(true, appointmentService.findAll(page, size, doctorId, statusId, date), "Lista de citas")
        );
    }

    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long branchId) {
            
        return ResponseEntity.ok(
                new ApiResponse<>(true, appointmentService.getCalendar(startDate, endDate, doctorId, branchId), "Citas de calendario obtenidas")
        );
    }

    @GetMapping("/available-slots")
    public ResponseEntity<ApiResponse<List<TimeSlotResponse>>> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
            
        return ResponseEntity.ok(
                new ApiResponse<>(true, appointmentService.getAvailableSlots(doctorId, date), "Slots de tiempo disponibles")
        );
    }

    @PutMapping("/{id}/reschedule")
    public ResponseEntity<ApiResponse<Void>> reschedule(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleAppointmentRequest request) {
            
        appointmentService.reschedule(id, request);
        return ResponseEntity.ok(
                new ApiResponse<>(true, null, "Cita reprogramada exitosamente")
        );
    }

    @PatchMapping("/{id}/flow-status")
    public ResponseEntity<ApiResponse<Void>> updateFlowStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAppointmentFlowRequest request) {
            
        appointmentService.updateFlowStatus(id, request);
        return ResponseEntity.ok(
                new ApiResponse<>(true, null, "Estado de flujo actualizado")
        );
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelAppointmentRequest request) {
            
        appointmentService.cancel(id, request);
        return ResponseEntity.ok(
                new ApiResponse<>(true, null, "Cita cancelada")
        );
    }
}
