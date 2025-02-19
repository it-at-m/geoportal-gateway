package de.swm.lhm.geoportal.gateway.filter.webfilter;

import de.swm.lhm.geoportal.gateway.geoservice.GeoServiceProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Trigger ArcGis client to send authentication credentials
 *
 * Normally ArcGis sends a getcapabilites request without authentication when adding
 * a WMS/WFS layer to a project. This filter here returns a HTTP-forbidden state on this
 * request, which then again triggers ArcGis to resend the request with the required credentials.
 *
 * Assuming a previously saved ArcGis-project gets loaded by a user, no getcapabilites
 * request will be send, and instead the layer will be directly accessed using "GetMap"
 * or "GetFeature" requests. Expecting the same two-requests behaviour as detailed above,
 * this filter is implemented as webfilter instead of a gatewayfilter to ensure the
 * GeoServiceRequestAuthorizationManager does not block the request before a gatewayfilter
 * would be executed.
 */
public class ArcGisAuthenticationTriggerFilter implements WebFilter  {
    private final GeoServiceProperties geoServiceProperties;

    public ArcGisAuthenticationTriggerFilter(GeoServiceProperties geoServiceProperties) {
        this.geoServiceProperties = geoServiceProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (!isGeoserviceRequest(request) || !isArcGisRequest(request) || containsAuthentication(request)) {
            return chain.filter(exchange);
        }

        ServerHttpResponse response = exchange.getResponse();
        // trigger arcgis to send authentication credentials
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "basic");

        // no further dispatching along the filterChain
        return Mono.empty();
    }

    protected boolean isGeoserviceRequest(ServerHttpRequest request) {
        return StringUtils.startsWithIgnoreCase(
                request.getPath().value(),
                geoServiceProperties.getEndpoint()
        );
    }

    private boolean containsAuthentication(ServerHttpRequest request) {
        // also see comment on DropAuthHeaderGeoServiceGatewayFilter class
        List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        return authHeaders != null && !authHeaders.isEmpty();
    }

    private boolean isArcGisRequest(ServerHttpRequest request) {
        //  Example user-agent: 'ArcGIS Pro 2.0.0 (0000000000) - ArcGIS Pro'
        List<String> userAgentHeaders = request.getHeaders().get(HttpHeaders.USER_AGENT);
        if (userAgentHeaders == null || userAgentHeaders.isEmpty()) {
            return false;
        }

        return userAgentHeaders.stream()
                .anyMatch(ua -> StringUtils.containsIgnoreCase(ua, "ArcGIS"));
    }
}
