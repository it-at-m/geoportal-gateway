package de.swm.lhm.geoportal.gateway.geoservice.inspect;

import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;

@EqualsAndHashCode
@Setter
@Getter
public class GeoServiceRequest {

    private HttpMethod httpMethod;

    private ServiceType serviceType;

    private GeoServiceEndpoint endpoint;

    private Optional<String> requestType = Optional.empty();


    /**
     * workspace name derived from the request path
     */
    private Optional<String> workspaceName = Optional.empty();

    /**
     * Query-parameter map with lowercased parameter names
     */
    @Setter(AccessLevel.NONE)
    private Map<String, String> paramsNormalized;

    private Set<QualifiedLayerName> layers = new HashSet<>();


    public void setParamsNormalized(Map<String, String> params) {
        paramsNormalized = new HashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            paramsNormalized.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
        }
    }

    public void setParamsNormalized(MultiValueMap<String, String> params) {
        // removing duplicate parameters, as these are not really needed for this usecase
        //
        // Depending on how geoserver handles duplicate parameters there may be security issues
        // in cases of duplicate parameters like "layers", so these are handled directly in the inspector class
        setParamsNormalized(params.toSingleValueMap());
    }

    public void addLayers(Collection<QualifiedLayerName> newLayers) {
        layers = Stream.concat(layers.stream(), newLayers.stream()).collect(Collectors.toSet());
    }

    public Optional<GeoServiceRequestType> geoServiceRequestType() {
        return getRequestType().flatMap(GeoServiceRequestType::getFromString);
    }

    public boolean is(ServiceType serviceType, GeoServiceRequestType geoServiceRequestType) {
        return getServiceType().equals(serviceType) && is(geoServiceRequestType);
    }

    public boolean is(GeoServiceRequestType geoServiceRequestType) {
        return geoServiceRequestType()
                .map(gsrt -> gsrt.equals(geoServiceRequestType))
                .orElse(false);
    }
}
