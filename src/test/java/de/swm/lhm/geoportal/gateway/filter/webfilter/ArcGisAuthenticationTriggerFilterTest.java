package de.swm.lhm.geoportal.gateway.filter.webfilter;

import de.swm.lhm.geoportal.gateway.geoservice.GeoServiceProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ArcGisAuthenticationTriggerFilterTest {

    public static final String ARC_GIS_USER_AGENT = "ArcGIS Pandora Edition";

    GeoServiceProperties getGeoServiceProperties() {
        GeoServiceProperties geoServiceProperties = new GeoServiceProperties();
        geoServiceProperties.setEndpoint("/geoserver");
        return geoServiceProperties;
    }

    @Test
    void arcGisUserAgentIsForcedToAuthenticate() {
        GeoServiceProperties geoserviceProperties = getGeoServiceProperties();
        ArcGisAuthenticationTriggerFilter filter = new ArcGisAuthenticationTriggerFilter(geoserviceProperties);


        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(String.format("%s/wms", geoserviceProperties.getEndpoint()))
                        .header(HttpHeaders.USER_AGENT, ARC_GIS_USER_AGENT)
        );

        AtomicBoolean requestIsForwardedToFollowingFilters = new AtomicBoolean(false);
        WebFilterChain filterChain = filterExchange -> {
            requestIsForwardedToFollowingFilters.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, filterChain).block();

        assertThat(exchange.getResponse().getStatusCode(), is(HttpStatus.UNAUTHORIZED));

        // request should not be sent to backend
        assertThat(requestIsForwardedToFollowingFilters.get(), is(false));
    }

    @Test
    void otherUserAgentIsNotForcedToAuthenticate() {
        GeoServiceProperties geoserviceProperties = getGeoServiceProperties();
        ArcGisAuthenticationTriggerFilter filter = new ArcGisAuthenticationTriggerFilter(geoserviceProperties);


        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(String.format("%s/wms", geoserviceProperties.getEndpoint()))
                        .header(HttpHeaders.USER_AGENT, "QGIS")
        );

        AtomicBoolean requestIsForwardedToFollowingFilters = new AtomicBoolean(false);
        AtomicBoolean filterChainCalled = new AtomicBoolean(false);
        WebFilterChain filterChain = filterExchange -> {
            filterChainCalled.set(true);
            requestIsForwardedToFollowingFilters.set(true);
            filterExchange.getResponse().setStatusCode(HttpStatus.I_AM_A_TEAPOT);
            return Mono.empty();
        };

        filter.filter(exchange, filterChain).block();

        assertThat(exchange.getResponse().getStatusCode(), is(HttpStatus.I_AM_A_TEAPOT));
        assertThat(filterChainCalled.get(), is(true));

        // request should be sent to backend
        assertThat(requestIsForwardedToFollowingFilters.get(), is(true));
    }

    @Test
    void arcGisUserAgentWithAuthenticationIsAllowed() {
        GeoServiceProperties geoserviceProperties = getGeoServiceProperties();
        ArcGisAuthenticationTriggerFilter filter = new ArcGisAuthenticationTriggerFilter(geoserviceProperties);


        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(String.format("%s/wms", geoserviceProperties.getEndpoint()))
                        .header(HttpHeaders.USER_AGENT, ARC_GIS_USER_AGENT)
                        .header(HttpHeaders.AUTHORIZATION, "does-not-matter")
        );

        AtomicBoolean requestIsForwardedToFollowingFilters = new AtomicBoolean(false);
        AtomicBoolean filterChainCalled = new AtomicBoolean(false);
        WebFilterChain filterChain = filterExchange -> {
            filterChainCalled.set(true);
            requestIsForwardedToFollowingFilters.set(true);
            filterExchange.getResponse().setStatusCode(HttpStatus.I_AM_A_TEAPOT);
            return Mono.empty();
        };

        filter.filter(exchange, filterChain).block();

        assertThat(exchange.getResponse().getStatusCode(), is(HttpStatus.I_AM_A_TEAPOT));
        assertThat(filterChainCalled.get(), is(true));

        // request should be sent to backend
        assertThat(requestIsForwardedToFollowingFilters.get(), is(true));
    }

}
