package de.swm.lhm.geoportal.gateway.geoservice.gfi;


import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;

record ReferencedColumnValue(QualifiedLayerName qualifiedLayerName, String columnName, String columnValue) {

    public ReferencedColumnValue replaceQualifiedLayerName(QualifiedLayerName newQualifiedLayerName) {
        return new ReferencedColumnValue(newQualifiedLayerName, columnName, columnValue);
    }
}
