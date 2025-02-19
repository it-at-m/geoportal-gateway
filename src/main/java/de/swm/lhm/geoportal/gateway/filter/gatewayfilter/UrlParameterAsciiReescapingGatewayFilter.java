package de.swm.lhm.geoportal.gateway.filter.gatewayfilter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UrlParameterAsciiReescapingGatewayFilter implements GatewayFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        URI uri = exchange.getRequest().getURI();

        try { // see org.springframework.cloud.gateway.filter.GatewayFilter.filter()
            String query = reEscapeUrlParameterAscii(uri.getRawQuery());

            URI newUri = UriComponentsBuilder
                    .fromUri(uri)
                    .replaceQuery(query)
                    .build(true)
                    .toUri();

            return chain.filter(exchange
                    .mutate()
                    .request(request.mutate().uri(newUri).build())
                    .build());
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Invalid URI query: \"" + uri.getRawQuery() + "\"");
        }
    }

    String reEscapeUrlParameterAscii(String query) {
        if (query == null) {
            return null;
        }
        return URLEncoder.encode(
                URLDecoder.decode(query, StandardCharsets.UTF_8),
                StandardCharsets.US_ASCII);
    }
}
