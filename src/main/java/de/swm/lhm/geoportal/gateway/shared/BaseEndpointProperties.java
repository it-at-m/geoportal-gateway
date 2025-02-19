package de.swm.lhm.geoportal.gateway.shared;

import de.swm.lhm.geoportal.gateway.util.EndpointUtils;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static de.swm.lhm.geoportal.gateway.util.EndpointUtils.prepareEndpoint;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseEndpointProperties {

    private String path;
    private String apiEndpoint;

    private String endpoint;
    private String endpointWithSlashes;
    private String endpointWithoutSlashes;
    private Integer endpointWithSlashesLength;

    @PostConstruct
    public void postInit() {

        EndpointUtils.Endpoints prepared = prepareEndpoint(endpoint);

        endpoint = prepared.getEndpointRaw();
        endpointWithSlashes = prepared.getEndpointWithSlashes();
        endpointWithoutSlashes = prepared.getEndpointWithoutSlashes();
        endpointWithSlashesLength = prepared.getEndpointWithSlashesLength();

    }

}