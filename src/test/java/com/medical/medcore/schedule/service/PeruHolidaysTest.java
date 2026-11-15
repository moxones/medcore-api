package com.medical.medcore.schedule.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PeruHolidaysTest {

    @Test
    void easterSundayMatchesKnownDates() {
        assertThat(PeruHolidays.easterSunday(2024)).isEqualTo(LocalDate.of(2024, 3, 31));
        assertThat(PeruHolidays.easterSunday(2026)).isEqualTo(LocalDate.of(2026, 4, 5));
        assertThat(PeruHolidays.easterSunday(2027)).isEqualTo(LocalDate.of(2027, 3, 28));
    }

    @Test
    void yearIncludesHolyWeekAndFixedHolidaysInOrder() {
        var holidays = PeruHolidays.forYear(2027);

        assertThat(holidays).hasSize(16);
        assertThat(holidays.get(0).date()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(holidays).anyMatch(h -> h.date().equals(LocalDate.of(2027, 3, 25)) && h.name().equals("Jueves Santo"));
        assertThat(holidays).anyMatch(h -> h.date().equals(LocalDate.of(2027, 12, 9)));
        assertThat(holidays).isSortedAccordingTo((a, b) -> a.date().compareTo(b.date()));
    }
}
