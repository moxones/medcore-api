package com.medical.medcore.schedule.dto;

import java.util.List;

public record HolidayImportResponse(
        int created,
        int skipped,
        int flagged,
        List<BranchClosureResponse> closures
) {
}
