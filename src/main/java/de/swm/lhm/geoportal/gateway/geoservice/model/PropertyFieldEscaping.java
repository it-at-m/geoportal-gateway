package de.swm.lhm.geoportal.gateway.geoservice.model;

import lombok.Getter;

@Getter
public enum PropertyFieldEscaping {
    FILE("file"),
    URL("url"),
    MAILTO("mailto"),
    NONE("none"),
    IMAGE("image"),
    NUMBER("number");

    private final String name;

    private PropertyFieldEscaping(String name) {
        this.name = name;
    }

    public static PropertyFieldEscaping fromName(String name) {
        if (name != null) {
            switch (name) {
                case "file":
                    return FILE;
                case "url":
                    return URL;
                case "mailto":
                    return MAILTO;
                case "none":
                    return NONE;
                case "image":
                    return IMAGE;
                case "number":
                    return NUMBER;
            }
        }
        throw new IllegalArgumentException("Unknown EscapeType '" + name + "'");
    }
}
