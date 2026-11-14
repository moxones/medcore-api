package com.medical.medcore.triage.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TriageUrgencyTest {

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "ESI, 1, CRITICAL",
            "ESI, 2, CRITICAL",
            "ESI, 3, URGENT",
            "ESI, 4, NORMAL",
            "ESI, 5, NORMAL",
            "null, 2, CRITICAL",
            "ESI, abc, NORMAL",
            "ESI, null, NORMAL",
            "MANCHESTER, RED, CRITICAL",
            "MANCHESTER, naranja, CRITICAL",
            "MANCHESTER, YELLOW, URGENT",
            "MANCHESTER, GREEN, NORMAL",
            "manchester, 3, URGENT"
    })
    void derivesUrgencyFromThePriorityScale(String system, String level, TriageUrgency expected) {
        assertThat(TriageUrgency.from(system, level)).isEqualTo(expected);
    }
}
