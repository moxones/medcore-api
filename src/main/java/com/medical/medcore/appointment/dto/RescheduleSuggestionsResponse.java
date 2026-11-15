package com.medical.medcore.appointment.dto;

import java.util.List;

public record RescheduleSuggestionsResponse(
        Long appointmentId,
        List<RescheduleSuggestionResponse> sameProfessional,
        List<RescheduleSuggestionResponse> otherProfessionals
) {
}
