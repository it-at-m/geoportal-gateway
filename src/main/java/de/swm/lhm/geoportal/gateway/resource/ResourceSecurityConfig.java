package de.swm.lhm.geoportal.gateway.resource;


import de.swm.lhm.geoportal.gateway.authorization.HasAuthorization;
import de.swm.lhm.geoportal.gateway.resource.authorization.ResourceRequestAuthorizationManager;
import de.swm.lhm.geoportal.gateway.resource.authorization.ResourceRequestMatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ResourceSecurityConfig {

    @Bean
    HasAuthorization getResourceAuthorization(ResourceRequestMatcher matcher, ResourceRequestAuthorizationManager manager) {
        return HasAuthorization.AuthorizationBuilder
                .builder()
                .matcher(matcher)
                .manager(manager)
                .packageRef(this.getClass().getPackageName())
                .build();
    }

}
