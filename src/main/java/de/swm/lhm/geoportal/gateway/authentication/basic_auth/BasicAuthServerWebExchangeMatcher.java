package de.swm.lhm.geoportal.gateway.authentication.basic_auth;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

public class BasicAuthServerWebExchangeMatcher implements ServerWebExchangeMatcher {
    @Override
    public Mono<MatchResult> matches(ServerWebExchange exchange) {
        List<String> headerValues = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (headerValues == null || headerValues.isEmpty()) {
            return MatchResult.notMatch();
        }

        if (headerValues.stream().anyMatch(headerValue -> StringUtils.startsWithIgnoreCase(headerValue, "basic "))) {
            return MatchResult.match(Collections.emptyMap());
        } else {
            return MatchResult.notMatch();
        }
    }
}
