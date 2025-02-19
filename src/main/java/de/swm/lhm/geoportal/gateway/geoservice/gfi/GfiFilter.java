package de.swm.lhm.geoportal.gateway.geoservice.gfi;

import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequest;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

public interface GfiFilter {

    boolean supportsFormat(String contentType);

    boolean supportsMediaType(MediaType mediaType);

    Mono<String> filterGetFeatureInfoBody(GeoServiceRequest geoServiceRequest, String body);
}
