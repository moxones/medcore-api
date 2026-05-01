package com.medical.medcore.service.appointment;

import com.medical.medcore.dto.request.CancelAppointmentRequest;
import com.medical.medcore.dto.request.CreateAppointmentRequest;
import com.medical.medcore.dto.request.RescheduleAppointmentRequest;
import com.medical.medcore.dto.request.UpdateAppointmentFlowRequest;
import com.medical.medcore.dto.response.AppointmentResponse;
import com.medical.medcore.dto.response.TimeSlotResponse;
import com.medical.medcore.types.PageableResponse;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {
    AppointmentResponse create(CreateAppointmentRequest request);
    
    PageableResponse<AppointmentResponse> findAll(int page, int size, Long doctorId, Long statusId, LocalDate date);
    
    List<AppointmentResponse> getCalendar(LocalDate startDate, LocalDate endDate, Long doctorId, Long branchId);
    
    List<TimeSlotResponse> getAvailableSlots(Long doctorId, LocalDate date);
    
    void reschedule(Long id, RescheduleAppointmentRequest request);
    
    void updateFlowStatus(Long id, UpdateAppointmentFlowRequest request);
    
    void cancel(Long id, CancelAppointmentRequest request);
}
