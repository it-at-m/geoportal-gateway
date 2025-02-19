package de.swm.lhm.geoportal.gateway.m2m.authorization;

import de.swm.lhm.geoportal.gateway.m2m.M2MProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Setter
@Getter
public class M2MRequestMatcher implements ServerWebExchangeMatcher {
    private final M2MProperties m2MProperties;

    @Override
    public Mono<MatchResult> matches(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        if (request.getPath().value().startsWith(m2MProperties.getPasswordEndpoint())) {
            return MatchResult.match();
        }
        return MatchResult.notMatch();
    }
}
