package com.medical.medcore.appointment.controller;

import com.medical.medcore.appointment.dto.AppointmentFlowEventResponse;
import com.medical.medcore.appointment.dto.AppointmentResponse;
import com.medical.medcore.appointment.dto.AvailabilitySlotResponse;
import com.medical.medcore.appointment.dto.CancelAppointmentRequest;
import com.medical.medcore.appointment.dto.CreateAppointmentRequest;
import com.medical.medcore.appointment.dto.DayAvailabilityResponse;
import com.medical.medcore.appointment.dto.PublicBookingRequest;
import com.medical.medcore.appointment.dto.PublicBookingResponse;
import com.medical.medcore.appointment.dto.RescheduleAppointmentRequest;
import com.medical.medcore.appointment.dto.SpecialtySummaryResponse;
import com.medical.medcore.appointment.dto.TimeSlotResponse;
import com.medical.medcore.appointment.dto.UpdateAppointmentFlowRequest;
import com.medical.medcore.appointment.service.AppointmentService;
import com.medical.medcore.appointment.service.AvailabilityService;
import com.medical.medcore.appointment.service.PublicBookingService;
import com.medical.medcore.shared.api.ApiResponse;
import com.medical.medcore.shared.api.PageableResponse;
import com.medical.medcore.shared.security.annotation.RequireBooker;
import com.medical.medcore.shared.security.annotation.RequireStaff;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AvailabilityService availabilityService;
    private final PublicBookingService publicBookingService;

    @RequireBooker
    @PostMapping("/appointments")
    public ApiResponse<AppointmentResponse> create(@Valid @RequestBody CreateAppointmentRequest request) {
        return ApiResponse.ok(appointmentService.create(request), "Cita creada correctamente");
    }

    @PostMapping("/public/bookings")
    public ApiResponse<PublicBookingResponse> publicBooking(@Valid @RequestBody PublicBookingRequest request) {
        return ApiResponse.ok(publicBookingService.registerAndBook(request), "Cuenta creada y cita reservada");
    }

    @RequireStaff
    @GetMapping("/appointments")
    public ApiResponse<PageableResponse<AppointmentResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long statusId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String flowStatus) {
        return ApiResponse.ok(
                appointmentService.findAll(page, size, doctorId, patientId, statusId, status, date, flowStatus),
                "Lista de citas");
    }

    @GetMapping("/patients/me/appointments")
    public ApiResponse<PageableResponse<AppointmentResponse>> findMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long statusId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String flowStatus) {
        return ApiResponse.ok(appointmentService.findMine(page, size, statusId, status, date, flowStatus), "Mis citas");
    }

    @RequireStaff
    @GetMapping("/appointments/calendar")
    public ApiResponse<List<AppointmentResponse>> getCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long branchId) {
        return ApiResponse.ok(appointmentService.getCalendar(startDate, endDate, doctorId, branchId), "Calendario de citas");
    }

    @RequireStaff
    @GetMapping("/appointments/queue")
    public ApiResponse<List<AppointmentResponse>> getQueue(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(appointmentService.getQueue(branchId, doctorId, date), "Cola de atención");
    }

    @GetMapping("/appointments/available-slots")
    public ApiResponse<List<TimeSlotResponse>> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(availabilityService.getAvailableSlots(doctorId, branchId, date), "Horarios disponibles");
    }

    @GetMapping("/appointments/availability")
    public ApiResponse<List<DayAvailabilityResponse>> getAvailability(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long specialtyId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long appointmentTypeId) {
        return ApiResponse.ok(availabilityService.getAvailability(
                branchId, fromDate, toDate, specialtyId, doctorId, appointmentTypeId), "Disponibilidad");
    }

    @GetMapping("/appointments/specialties-summary")
    public ApiResponse<List<SpecialtySummaryResponse>> getSpecialtiesSummary(@RequestParam Long branchId) {
        return ApiResponse.ok(availabilityService.getSpecialtiesSummary(branchId), "Especialidades disponibles");
    }

    @RequireBooker
    @GetMapping("/appointments/{id}")
    public ApiResponse<AppointmentResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(appointmentService.findById(id), "Cita encontrada");
    }

    @RequireStaff
    @GetMapping("/appointments/{id}/history")
    public ApiResponse<List<AppointmentFlowEventResponse>> history(@PathVariable Long id) {
        return ApiResponse.ok(appointmentService.history(id), "Historial de la cita");
    }

    @RequireBooker
    @PutMapping("/appointments/{id}/reschedule")
    public ApiResponse<AppointmentResponse> reschedule(@PathVariable Long id,
                                                       @Valid @RequestBody RescheduleAppointmentRequest request) {
        return ApiResponse.ok(appointmentService.reschedule(id, request), "Cita reprogramada");
    }

    @RequireStaff
    @PatchMapping("/appointments/{id}/flow-status")
    public ApiResponse<AppointmentResponse> updateFlowStatus(@PathVariable Long id,
                                                             @Valid @RequestBody UpdateAppointmentFlowRequest request) {
        return ApiResponse.ok(appointmentService.updateFlowStatus(id, request.flowStatus()), "Estado de flujo actualizado");
    }

    @RequireBooker
    @PostMapping("/appointments/{id}/cancel")
    public ApiResponse<AppointmentResponse> cancel(@PathVariable Long id,
                                                   @Valid @RequestBody CancelAppointmentRequest request) {
        return ApiResponse.ok(appointmentService.cancel(id, request), "Cita cancelada");
    }

    @RequireStaff
    @PostMapping("/appointments/{id}/no-show")
    public ApiResponse<AppointmentResponse> markNoShow(@PathVariable Long id) {
        return ApiResponse.ok(appointmentService.markNoShow(id), "Inasistencia registrada");
    }
}
