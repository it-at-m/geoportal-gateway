package de.swm.lhm.geoportal.gateway.authorization.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum StorkQaaLevel {

    /*
    Schlüssel__________Vertrauensniveau__Beschreibung
    STORK-QAA-Level-1__niedrig___________Benutzername/Passwort
    STORK-QAA-Level-3__substanziell______ELSTER-Zertifikat
    STORK-QAA-Level-4__hoch______________Online-Ausweisfunktion (Elektronischer Personalausweis, EU-Karte, …)
    */

    STORK_QAA_LEVEL_1("STORK-QAA-Level-1"),
    STORK_QAA_LEVEL_3("STORK-QAA-Level-3"),
    STORK_QAA_LEVEL_4("STORK-QAA-Level-4");

    private final String value;

    StorkQaaLevel(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static StorkQaaLevel fromValue(Object rawValue) {

        String value;

        switch (rawValue) {
            case String str ->
                value = str;
            case Enum<?> enumValue ->
                value = enumValue.name();
            default ->
                value = rawValue.toString();
        }

        for (StorkQaaLevel level : values()) {
            if (level.value.equals(value) || level.name().equals(value)) {
                return level;
            }
        }

        throw new IllegalArgumentException("Unknown value: %s".formatted(value));
    }

}
