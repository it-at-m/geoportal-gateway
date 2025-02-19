package de.swm.lhm.geoportal.gateway.print.filter;

import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import de.swm.lhm.geoportal.gateway.geoservice.GeoServiceProperties;
import de.swm.lhm.geoportal.gateway.geoservice.HostReplacer;
import de.swm.lhm.geoportal.gateway.util.DataBufferUtils;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.TestPropertySource;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

class JsonReplacePublicHostnamesGatewayFilterTest extends BaseIntegrationTest {


    @Test
    void publicHostNamesAreReplaced() {
        GeoServiceProperties geoServiceProperties = new GeoServiceProperties();
        geoServiceProperties.setHostnameMapping("http://maps6.geosolutionsgroup.com,http://maps.somewhereelse.de");

        HostReplacer hostReplacer = new HostReplacer(geoServiceProperties);

        String inputJson = "{\"some_url\":\"http://maps.somewhereelse.de\"}";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("http://localhost:8080")
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(inputJson)
        );

        AtomicBoolean requestIsForwardedToFollowingFilters = new AtomicBoolean(false);
        GatewayFilterChain filterChain = filterExchange -> {
            ServerHttpRequest request = filterExchange.getRequest();

            return DataBufferUtils.copyAsString(request.getBody())
                    .map(outputJson -> {
                        requestIsForwardedToFollowingFilters.set(true);
                        assertThat(outputJson, is(not(containsString("http://maps.somewhereelse.de"))));
                        assertThat(outputJson, is(containsString("http://maps6.geosolutionsgroup.com")));
                        return outputJson;
                    })
                    .then();
        };

        JsonReplacePublicHostnamesGatewayFilter filter = new JsonReplacePublicHostnamesGatewayFilter(
                hostReplacer
        );
        filter.filter(exchange, filterChain).block();

        assertThat(requestIsForwardedToFollowingFilters.get(), is(true));
    }
}
