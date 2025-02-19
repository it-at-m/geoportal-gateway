package de.swm.lhm.geoportal.gateway.print;

import de.swm.lhm.geoportal.gateway.authorization.HasAuthorization;
import de.swm.lhm.geoportal.gateway.print.authorization.PrintRequestAuthorizationManager;
import de.swm.lhm.geoportal.gateway.print.authorization.PrintRequestMatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class PrintSecurityConfig {

    @Bean
    HasAuthorization getPrintAuthorization(PrintRequestMatcher matcher, PrintRequestAuthorizationManager manager) {
        return HasAuthorization.AuthorizationBuilder
                .builder()
                .matcher(matcher)
                .manager(manager)
                .packageRef(this.getClass().getPackageName())
                .build();
    }

}
