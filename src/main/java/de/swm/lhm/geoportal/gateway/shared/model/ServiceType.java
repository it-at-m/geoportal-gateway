package de.swm.lhm.geoportal.gateway.shared.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Schema(description = "Service Typ")
@Getter
@RequiredArgsConstructor
public enum ServiceType {

    WMS("1.3.0", ServerType.GEOSERVER, false),
    WFS("2.0.0", ServerType.GEOSERVER, false),
    WMTS("1.0.0", ServerType.GEOSERVER, false),
    WMS_WFS("", ServerType.GEOSERVER, true),
    STA("1.1", ServerType.FROST, false),
    GEN("", ServerType.ANY, false);

    private final String version;
    private final ServerType type;
    private final Boolean virtual;

    public enum ServerType {
        GEOSERVER,
        FROST,
        ANY
    }

    public static List<ServiceType> getByServerType(ServerType type){
        return Arrays.stream(ServiceType.values())
                .filter(service -> service.getType().equals(type))
                .toList();
    }

    public static List<ServiceType> getAllGeoServerServices(){
        return getByServerType(ServerType.GEOSERVER);
    }
}
