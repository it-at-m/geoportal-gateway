package de.swm.lhm.geoportal.gateway.m2m;

import de.swm.lhm.geoportal.gateway.authorization.HasAuthorization;
import de.swm.lhm.geoportal.gateway.m2m.authorization.M2MAuthorizationManager;
import de.swm.lhm.geoportal.gateway.m2m.authorization.M2MRequestMatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class M2MSecurityConfig {
    @Bean
    HasAuthorization getM2MAuthorization(M2MRequestMatcher m2MRequestMatcher, M2MAuthorizationManager m2MAuthorizationManager) {
        return HasAuthorization.AuthorizationBuilder
                .builder()
                .matcher(m2MRequestMatcher)
                .manager(m2MAuthorizationManager)
                .packageRef(this.getClass().getPackageName())
                .build();
    }
}
