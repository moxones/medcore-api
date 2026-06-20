package com.medical.medcore.dto.response;

public record DoctorProductivityResponse(
        Long doctorId,
        String doctorName,
        String specialties,
        long totalAppointments,
        long completedAppointments,
        long cancelledAppointments,
        long noShowAppointments,
        long uniquePatients,
        long avgConsultationMinutes,
        double completionRate,
        double noShowRate
) {}
