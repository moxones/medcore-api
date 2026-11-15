package com.medical.medcore.schedule.dto;

public record ClosureOutcomeResponse(
        BranchClosureResponse closure,
        int flagged,
        int cancelled,
        int restored
) {
}
