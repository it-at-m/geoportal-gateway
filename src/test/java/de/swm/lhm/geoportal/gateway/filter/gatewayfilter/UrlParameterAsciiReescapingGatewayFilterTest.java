package de.swm.lhm.geoportal.gateway.filter.gatewayfilter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;


class UrlParameterAsciiReescapingGatewayFilterTest {

    static class TestUrlParameterAsciiReescapingGatewayFilter extends UrlParameterAsciiReescapingGatewayFilter {
        @Override
        String reEscapeUrlParameterAscii(String query) {
            throw new RuntimeException("Simulated exception");
        }
    }

    @Test
    void testFilterThrowsIllegalStateException() {
        // Arrange
        UrlParameterAsciiReescapingGatewayFilter filter = new TestUrlParameterAsciiReescapingGatewayFilter();

        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(exchange.getRequest()).thenReturn(request);

        URI uri = URI.create("https://test.com");
        when(request.getURI()).thenReturn(uri);

        assertThrows(IllegalStateException.class, () -> filter.filter(exchange, chain).block());
    }

}