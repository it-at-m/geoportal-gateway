package de.swm.lhm.geoportal.gateway.portal;


import de.swm.lhm.geoportal.gateway.authorization.HasAuthorization;
import de.swm.lhm.geoportal.gateway.portal.authorization.PortalRequestAuthorizationManager;
import de.swm.lhm.geoportal.gateway.portal.authorization.PortalRequestMatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class PortalSecurityConfig {

    @Bean
    HasAuthorization getPortalAuthorization(PortalRequestMatcher matcher, PortalRequestAuthorizationManager manager) {
        return HasAuthorization.AuthorizationBuilder
                .builder()
                .matcher(matcher)
                .manager(manager)
                .packageRef(this.getClass().getPackageName())
                .build();
    }

}
