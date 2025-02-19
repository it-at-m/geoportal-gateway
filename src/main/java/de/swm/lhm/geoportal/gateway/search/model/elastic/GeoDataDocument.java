package de.swm.lhm.geoportal.gateway.search.model.elastic;

import lombok.Data;

@Data
public class GeoDataDocument {
    private String id;
    private String fieldName;
    private String layerId;
    private String geoDataValue;
    private Integer resourceId;
    private Double topValue;
    private Double rightValue;
    private String layerTitle;
}
