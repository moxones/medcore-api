package com.medical.medcore.dto.response;

import java.util.List;

/**
 * Resumen del dashboard del portal del médico (`GET /dashboard/doctor-summary`).
 * Resuelve al médico autenticado desde el token; no recibe doctorId por parámetro.
 */
public record DoctorDashboardSummaryResponse(
        long totalToday,
        long upcoming,
        long waiting,
        long inProgress,
        long completedToday,
        long pendingNotes,
        long avgConsultationMinutes,
        DoctorAgendaItemResponse nextPatient,
        List<DoctorAgendaItemResponse> agenda,
        List<DoctorRecentPatientResponse> recentPatients
) {}
