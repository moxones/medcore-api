package com.medical.medcore.schedule.dto;

import java.util.List;

public record ImpactPreviewResponse(
        int affectedCount,
        List<AffectedAppointmentResponse> appointments,
        boolean requiresApproval,
        boolean canCancelAppointments,
        List<String> warnings
) {
}
