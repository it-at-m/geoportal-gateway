package de.swm.lhm.geoportal.gateway.util;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;


@UtilityClass
public class EndpointUtils {

    public static Endpoints prepareEndpoint(String endpoint) {
        if (StringUtils.isBlank(endpoint)) {
            return Endpoints.builder()
                    .endpointRaw(endpoint)
                    .endpointWithoutSlashes("")
                    .endpointWithSlashes("/")
                    .endpointWithSlashesLength(1)
                    .build();
        } else {
            String endpointWithSlashed = endpoint.trim();
            String endpointWithoutSlashes = endpoint.trim();

            if (!endpointWithSlashed.startsWith("/")) {
                endpointWithSlashed = "/" + endpointWithSlashed;
            } else {
                endpointWithoutSlashes = endpointWithoutSlashes.substring(1);
            }

            if (!endpointWithSlashed.endsWith("/")) {
                endpointWithSlashed += "/";
            }
            if (endpointWithoutSlashes.endsWith("/")) {
                endpointWithoutSlashes = endpointWithoutSlashes.substring(0, endpointWithoutSlashes.length() - 1);
            }

            return Endpoints.builder()
                    .endpointRaw(endpoint)
                    .endpointWithoutSlashes(endpointWithoutSlashes)
                    .endpointWithSlashes(endpointWithSlashed)
                    .endpointWithSlashesLength(endpointWithSlashed.length())
                    .build();
        }
    }

    @Data
    @Builder
    public static class Endpoints {
        private String endpointRaw;
        private String endpointWithSlashes;
        private String endpointWithoutSlashes;
        private Integer endpointWithSlashesLength;
    }

}
