package de.swm.lhm.geoportal.gateway.geoservice;

import de.swm.lhm.geoportal.gateway.authorization.HasAuthorization;
import de.swm.lhm.geoportal.gateway.geoservice.authorization.GeoServiceRequestAuthorizationManager;
import de.swm.lhm.geoportal.gateway.geoservice.authorization.GeoServiceRequestMatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeoServiceSecurityConfig {

    @Bean
    HasAuthorization getGeoServiceAuthorization(GeoServiceRequestMatcher geoServiceRequestMatcher, GeoServiceRequestAuthorizationManager geoServiceRequestAuthorizationManager) {
        return HasAuthorization.AuthorizationBuilder
                .builder()
                .matcher(geoServiceRequestMatcher)
                .manager(geoServiceRequestAuthorizationManager)
                .packageRef(this.getClass().getPackageName())
                .build();
    }
}
