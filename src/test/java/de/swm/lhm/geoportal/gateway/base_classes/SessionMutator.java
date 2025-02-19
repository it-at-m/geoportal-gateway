package de.swm.lhm.geoportal.gateway.base_classes;

import com.google.common.collect.ImmutableMap;
import jakarta.annotation.Nonnull;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.lang.Nullable;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import reactor.core.publisher.Mono;

import java.util.Map;

// https://stackoverflow.com/questions/50214030/accessing-the-websession-in-webfluxtest

public class SessionMutator implements WebTestClientConfigurer {

    private final Map<String, Object> sessionMap;

    private SessionMutator(final Map<String, Object> sessionMap) {
        this.sessionMap = sessionMap;
    }

    public static SessionMutator sessionMutator(final Map<String, Object> sessionMap) {
        return new SessionMutator(sessionMap);
    }

    @Override
    public void afterConfigurerAdded(
            @Nullable final WebTestClient.Builder builder,
            final WebHttpHandlerBuilder httpHandlerBuilder,
            final ClientHttpConnector connector
    ) {
        final SessionMutatorFilter sessionMutatorFilter = new SessionMutatorFilter(sessionMap);
        assert httpHandlerBuilder != null;
        httpHandlerBuilder.filters(filters -> filters.addFirst(sessionMutatorFilter));
    }

    public static ImmutableMap.Builder<String, Object> sessionBuilder() {
        return new ImmutableMap.Builder<>();
    }

    private static class SessionMutatorFilter implements WebFilter {

        private final Map<String, Object> sessionMap;

        SessionMutatorFilter(Map<String, Object> sessionMap) {
            this.sessionMap = sessionMap;
        }

        @Override
        @Nonnull
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain webFilterChain) {
            return exchange.getSession()
                    .doOnNext(webSession -> webSession.getAttributes().putAll(sessionMap))
                    .then(webFilterChain.filter(exchange));
        }
    }
}