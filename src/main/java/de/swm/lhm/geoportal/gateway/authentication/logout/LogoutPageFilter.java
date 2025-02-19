package de.swm.lhm.geoportal.gateway.authentication.logout;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.server.authentication.logout.LogoutWebFilter;
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;


@Slf4j
public final class LogoutPageFilter extends LogoutWebFilter {

    public LogoutPageFilter(ServerLogoutHandler serverLogoutHandler, ServerLogoutSuccessHandler serverLogoutSuccessHandler) {
        super();
        setRequiresLogoutMatcher(ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/logout"));
        setLogoutSuccessHandler(serverLogoutSuccessHandler);
        setLogoutHandler(serverLogoutHandler);
    }

}
