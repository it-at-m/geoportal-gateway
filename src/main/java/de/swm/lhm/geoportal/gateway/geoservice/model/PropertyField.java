package de.swm.lhm.geoportal.gateway.geoservice.model;

public record PropertyField(
        String schemaName,
        String tableName,
        String fieldName,
        String label,
        boolean visible,
        PropertyFieldEscaping escaping
) {
}
