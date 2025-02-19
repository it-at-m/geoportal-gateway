package de.swm.lhm.geoportal.gateway.search;

import de.swm.lhm.geoportal.gateway.authorization.HasAuthorization;
import de.swm.lhm.geoportal.gateway.portal.authorization.PortalRequestAuthorizationManager;
import de.swm.lhm.geoportal.gateway.search.authorization.SearchRequestMatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SearchSecurityConfig {

    @Bean
    HasAuthorization getSearchAuthorization(SearchRequestMatcher matcher, PortalRequestAuthorizationManager manager) {
        return HasAuthorization.AuthorizationBuilder
                .builder()
                .matcher(matcher)
                .manager(manager)
                .packageRef(this.getClass().getPackageName())
                .build();
    }

}