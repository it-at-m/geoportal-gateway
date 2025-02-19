package de.swm.lhm.geoportal.gateway.authentication.login;

import de.swm.lhm.geoportal.gateway.authentication.LoginLogoutProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;


@Configuration
@RequiredArgsConstructor
public class LoginConfig {

    @Bean
    ServerAuthenticationSuccessHandler getLoginSuccessHandler(LoginLogoutProperties loginLogoutProperties) {
        return new RedirectServerAuthenticationSuccessHandler(loginLogoutProperties.getLoginSuccess().getEndpoint());
    }

}
