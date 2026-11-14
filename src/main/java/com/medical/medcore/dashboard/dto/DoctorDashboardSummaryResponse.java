package com.medical.medcore.dashboard.dto;

import java.util.List;

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
