package de.swm.lhm.geoportal.gateway.authorization;

import lombok.Builder;
import lombok.Getter;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

public interface HasAuthorization {

    ServerWebExchangeMatcher getMatcher();

    ReactiveAuthorizationManager<AuthorizationContext> getManager();

    String info();

    @Builder
    @Getter
    class AuthorizationBuilder implements HasAuthorization {

        ServerWebExchangeMatcher matcher;
        ReactiveAuthorizationManager<AuthorizationContext> manager;
        String packageRef;

        @Override
        public String info() {
            return "matcher: " + this.matcher.getClass().getSimpleName() + ", manager: " + this.manager.getClass().getSimpleName() + ", package: " + this.packageRef;
        }
    }

}
