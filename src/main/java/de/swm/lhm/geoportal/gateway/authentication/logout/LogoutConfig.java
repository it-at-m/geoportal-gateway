package de.swm.lhm.geoportal.gateway.authentication.logout;

import de.swm.lhm.geoportal.gateway.authentication.keycloak.KeyCloakProviderProperties;
import de.swm.lhm.geoportal.gateway.authentication.LoginLogoutProperties;
import de.swm.lhm.geoportal.gateway.authorization.IAuthService;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;


@Configuration
@RequiredArgsConstructor
public class LogoutConfig {

    @Bean("dzmLogoutSuccessHandler")
    ServerLogoutSuccessHandler getLogoutSuccessHandler(
            KeyCloakProviderProperties keycloakProviderProperties,
            GatewayService gatewayService,
            LoginLogoutProperties loginLogoutProperties
    ) {

        KeycloakServerRedirectStrategy redirectStrategy = new KeycloakServerRedirectStrategy(
                keycloakProviderProperties,
                gatewayService,
                loginLogoutProperties
        );

        return new KeycloakRedirectLogoutSuccessHandler(redirectStrategy);
    }

    @Bean("dzmLogoutHandler")
    ServerLogoutHandler logoutHandler(IAuthService authorizationService) {
        return new KeycloakLogoutHandler(authorizationService);
    }

    @Bean
    @DependsOn({"dzmLogoutHandler", "dzmLogoutSuccessHandler"})
    LogoutPageFilter logoutPageFilter(
            @Qualifier("dzmLogoutSuccessHandler") ServerLogoutSuccessHandler serverLogoutSuccessHandler,
            @Qualifier("dzmLogoutHandler") ServerLogoutHandler serverLogoutHandler
    ){
        return new LogoutPageFilter(serverLogoutHandler, serverLogoutSuccessHandler);
    }

}
