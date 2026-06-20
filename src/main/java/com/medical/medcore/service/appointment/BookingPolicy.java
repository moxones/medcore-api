package com.medical.medcore.service.appointment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@Component
public class BookingPolicy {

    private final int leadTimeMinutes;
    private final Clock clock;

    @Autowired
    public BookingPolicy(
            @Value("${booking.lead-time-minutes:15}") int leadTimeMinutes,
            @Value("${booking.time-zone:America/Lima}") String clinicZone) {
        this(leadTimeMinutes, Clock.system(ZoneId.of(clinicZone)));
    }

    BookingPolicy(int leadTimeMinutes, Clock clock) {
        this.leadTimeMinutes = leadTimeMinutes;
        this.clock = clock;
    }

    public int getLeadTimeMinutes() {
        return leadTimeMinutes;
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public boolean isToday(LocalDateTime dateTime) {
        return dateTime != null && dateTime.toLocalDate().equals(today());
    }

    public LocalDateTime earliestBookable() {
        return now().plusMinutes(leadTimeMinutes);
    }

    public boolean isBookable(LocalDateTime slotStart) {
        return slotStart != null && !slotStart.isBefore(earliestBookable());
    }

    public boolean isBookable(LocalDate date, LocalTime startTime) {
        return isBookable(LocalDateTime.of(date, startTime));
    }
}
