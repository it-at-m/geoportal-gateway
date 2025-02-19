package de.swm.lhm.geoportal.gateway.authorization.model;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StorkQaaLevelTest {

    @Test
    void fromValueShouldReturnEnumForValidString() {
        // Test für gültige String-Eingaben
        assertThat(StorkQaaLevel.fromValue("STORK-QAA-Level-1"), is(StorkQaaLevel.STORK_QAA_LEVEL_1));
        assertThat(StorkQaaLevel.fromValue("STORK-QAA-Level-3"), is(StorkQaaLevel.STORK_QAA_LEVEL_3));
        assertThat(StorkQaaLevel.fromValue("STORK-QAA-Level-4"), is(StorkQaaLevel.STORK_QAA_LEVEL_4));
    }

    @Test
    void fromValueShouldReturnEnumForValidEnum() {
        // Test für gültige Enum-Eingaben
        assertThat(StorkQaaLevel.fromValue(StorkQaaLevel.STORK_QAA_LEVEL_1), is(StorkQaaLevel.STORK_QAA_LEVEL_1));
        assertThat(StorkQaaLevel.fromValue(StorkQaaLevel.STORK_QAA_LEVEL_3), is(StorkQaaLevel.STORK_QAA_LEVEL_3));
        assertThat(StorkQaaLevel.fromValue(StorkQaaLevel.STORK_QAA_LEVEL_4), is(StorkQaaLevel.STORK_QAA_LEVEL_4));
    }

    @Test
    void fromValueShouldReturnEnumForEnumName() {
        // Test für Eingaben, die den Enum-Namen entsprechen
        assertThat(StorkQaaLevel.fromValue("STORK_QAA_LEVEL_1"), is(StorkQaaLevel.STORK_QAA_LEVEL_1));
        assertThat(StorkQaaLevel.fromValue("STORK_QAA_LEVEL_3"), is(StorkQaaLevel.STORK_QAA_LEVEL_3));
        assertThat(StorkQaaLevel.fromValue("STORK_QAA_LEVEL_4"), is(StorkQaaLevel.STORK_QAA_LEVEL_4));
    }

    @Test
    void fromValueShouldThrowExceptionForInvalidString() {
        // Test für ungültige String-Eingaben
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            StorkQaaLevel.fromValue("INVALID")
        );
        assertThat(exception.getMessage(), containsString("Unknown value: INVALID"));
    }

    @Test
    void fromValueShouldThrowExceptionForInvalidType() {
        // Test für ungültige Typ-Eingaben
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            StorkQaaLevel.fromValue(12345)
        );
        assertThat(exception.getMessage(), containsString("Unknown value: 12345"));
    }
}