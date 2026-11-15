package com.medical.medcore.schedule.domain;

public enum ExceptionStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED;

    public boolean isOpen() {
        return this == PENDING || this == APPROVED;
    }
}
