package de.swm.lhm.geoportal.gateway.product.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum License {
    CC_BY_40("CC-BY Version 4.0"),
    DL_20("Deutschland Lizenz 2.0"),
    NB_LHM("Nutzungsbedingungen der LHM");

    private final String description;

    License(String description) {
        this.description = description;
    }

    @JsonValue
    public String getDescription() {
        return description;
    }

}