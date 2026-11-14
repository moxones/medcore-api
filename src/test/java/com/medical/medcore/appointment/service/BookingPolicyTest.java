package com.medical.medcore.appointment.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class BookingPolicyTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 10, 10, 0);
    private static final Clock FIXED = Clock.fixed(NOW.atZone(LIMA).toInstant(), LIMA);

    private final BookingPolicy policy = new BookingPolicy(15, FIXED);

    @Test
    void patientsNeedTheConfiguredLeadTime() {
        assertThat(policy.isBookable(NOW.plusMinutes(14))).isFalse();
        assertThat(policy.isBookable(NOW.plusMinutes(15))).isTrue();
        assertThat(policy.isBookable(NOW.minusMinutes(1))).isFalse();
        assertThat(policy.isBookable((LocalDateTime) null)).isFalse();
    }

    @Test
    void staffCanBookASlotThatIsStillRunning() {
        assertThat(policy.isBookableByStaff(NOW.minusMinutes(10), 30)).isTrue();
        assertThat(policy.isBookableByStaff(NOW.minusMinutes(30), 30)).isFalse();
        assertThat(policy.isBookableByStaff(NOW.plusDays(1), 30)).isTrue();
    }

    @Test
    void usesTheClinicClockForToday() {
        Clock lateUtc = Clock.fixed(Instant.parse("2026-03-11T03:30:00Z"), LIMA);
        BookingPolicy lateEvening = new BookingPolicy(15, lateUtc);

        assertThat(lateEvening.today()).isEqualTo(LocalDate.of(2026, 3, 10));
        assertThat(lateEvening.isToday(LocalDateTime.of(2026, 3, 10, 23, 0))).isTrue();
        assertThat(lateEvening.isToday(LocalDateTime.of(2026, 3, 11, 0, 30))).isFalse();
        assertThat(lateEvening.isToday(null)).isFalse();
    }
}
