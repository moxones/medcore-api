package com.medical.medcore.schedule.dto;

import jakarta.validation.constraints.Size;

public record ReviewRequest(@Size(max = 255) String note) {
}
