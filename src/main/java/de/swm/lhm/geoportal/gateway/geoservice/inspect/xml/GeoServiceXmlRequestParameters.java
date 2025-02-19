package de.swm.lhm.geoportal.gateway.geoservice.inspect.xml;

import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequestType;
import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@Setter
@Getter
public class GeoServiceXmlRequestParameters {

    GeoServiceRequestType geoServiceRequestType;

    ServiceType geoServiceType;

    Set<String> requestedLayers = new HashSet<>();
}
