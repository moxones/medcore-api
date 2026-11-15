package com.medical.medcore.schedule.service;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public record WorkWindow(LocalTime start, LocalTime end, int slotMinutes, Source source, Long exceptionId) {

    public enum Source {
        SCHEDULE,
        EXTRA
    }

    public long minutes() {
        return ChronoUnit.MINUTES.between(start, end);
    }

    public boolean fits(LocalTime from, int durationMinutes) {
        long startOffset = ChronoUnit.MINUTES.between(start, from);
        long available = ChronoUnit.MINUTES.between(from, end);
        return startOffset >= 0 && available >= durationMinutes;
    }

    public boolean contains(LocalTime from, LocalTime to) {
        return !from.isBefore(start) && !to.isAfter(end) && from.isBefore(to);
    }

    WorkWindow withBounds(LocalTime newStart, LocalTime newEnd) {
        return new WorkWindow(newStart, newEnd, slotMinutes, source, exceptionId);
    }
}
