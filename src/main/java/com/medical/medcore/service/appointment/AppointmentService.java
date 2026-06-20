package com.medical.medcore.service.appointment;

import com.medical.medcore.dto.request.CancelAppointmentRequest;
import com.medical.medcore.dto.request.CreateAppointmentRequest;
import com.medical.medcore.dto.request.RescheduleAppointmentRequest;
import com.medical.medcore.dto.request.UpdateAppointmentFlowRequest;
import com.medical.medcore.dto.response.AppointmentResponse;
import com.medical.medcore.dto.response.DayAvailabilityResponse;
import com.medical.medcore.dto.response.SpecialtySummaryResponse;
import com.medical.medcore.dto.response.TimeSlotResponse;
import com.medical.medcore.types.PageableResponse;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {
    AppointmentResponse create(CreateAppointmentRequest request);

    AppointmentResponse findById(Long id);

    PageableResponse<AppointmentResponse> findAll(int page, int size, Long doctorId, Long patientId, Long statusId, LocalDate date, String flowStatus);

    List<AppointmentResponse> getCalendar(LocalDate startDate, LocalDate endDate, Long doctorId, Long branchId);

    List<AppointmentResponse> getQueue(Long branchId, Long doctorId, LocalDate date);

    List<TimeSlotResponse> getAvailableSlots(Long doctorId, Long branchId, LocalDate date);

    List<DayAvailabilityResponse> getAvailability(Long branchId, LocalDate fromDate, LocalDate toDate,
                                                   Long specialtyId, Long doctorId, Long appointmentTypeId);

    List<SpecialtySummaryResponse> getSpecialtiesSummary(Long branchId);

    void reschedule(Long id, RescheduleAppointmentRequest request);

    AppointmentResponse updateFlowStatus(Long id, UpdateAppointmentFlowRequest request);

    void cancel(Long id, CancelAppointmentRequest request);

    List<AppointmentResponse> findPendingByDoctor(Long doctorId);
}
