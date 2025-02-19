package de.swm.lhm.geoportal.gateway.authorization;

import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import de.swm.lhm.geoportal.gateway.filter.webfilter.ArcGisAuthenticationTriggerFilter;
import de.swm.lhm.geoportal.gateway.filter.webfilter.BodyCachingFilter;
import de.swm.lhm.geoportal.gateway.authentication.login.LoginPageFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.client.web.server.OAuth2AuthorizationRequestRedirectWebFilter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.logout.LogoutWebFilter;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.ui.LoginPageGeneratingWebFilter;
import org.springframework.security.web.server.ui.LogoutPageGeneratingWebFilter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


@TestPropertySource(properties = {
        "geoportal.gateway.login.logout-redirect.endpoint=" + SecurityConfigTest.REDIRECT_ENDPOINT,
        "geoportal.gateway.login.logout-redirect.uri-key=" + SecurityConfigTest.REDIRECT_KEY
})
class SecurityConfigTest extends BaseIntegrationTest {

    public static final String REDIRECT_ENDPOINT = "/path/to/nowhere";
    public static final String REDIRECT_KEY = "redirect_to";
    @Autowired
    private List<SecurityWebFilterChain> filterChains;

    @Test
    void testFilterChainsLength(){

        assertThat(filterChains, hasSize(2));

    }

    @Test
    void testLoginFilter(){

        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

        filterChains.forEach(filterChain -> {
            Map<Integer, Object> orderedMap = getFilterOrder(filterChain);

            if (Boolean.TRUE.equals(filterChain.matches(exchange).block())){

                Integer loginPageFilterPosition = getFirstFilterPosition(orderedMap, LoginPageFilter.class);
                assertThat(loginPageFilterPosition, is(not(equalTo( -1))));

                Integer loginPageGeneratingWebFilterPosition = getFirstFilterPosition(orderedMap, LoginPageGeneratingWebFilter.class);
                assertThat(loginPageGeneratingWebFilterPosition, is(not(equalTo( -1))));

                assertThat(loginPageGeneratingWebFilterPosition, is(equalTo(loginPageFilterPosition + 1)));


            } else {

                Integer loginPageFilterPosition = getFirstFilterPosition(orderedMap, LoginPageFilter.class);
                assertThat(loginPageFilterPosition, is(equalTo( -1)));

            }
        });
    }

    @Test
    void testArcGisAuthFilter(){

        filterChains.forEach(filterChain -> {
            Map<Integer, Object> orderedMap = getFilterOrder(filterChain);

            List<Integer> allArcGisFilterPositions = getAllFilterPositions(orderedMap, ArcGisAuthenticationTriggerFilter.class);
            assertThat(allArcGisFilterPositions, hasSize(1));

            List<Integer> allBodyCachingFilterPositions = getAllFilterPositions(orderedMap, BodyCachingFilter.class);
            assertThat(allBodyCachingFilterPositions, hasSize(1));

            assertThat(allBodyCachingFilterPositions.getFirst(), is(equalTo(allArcGisFilterPositions.getFirst() + 1)));

        });
    }

    @Test
    void testBodyCachingFilter(){

        filterChains.forEach(filterChain -> {
            Map<Integer, Object> orderedMap = getFilterOrder(filterChain);

            List<Integer> allBodyCachingFilterPositions = getAllFilterPositions(orderedMap, BodyCachingFilter.class);
            assertThat(allBodyCachingFilterPositions, hasSize(1));

            Integer authenticationWebFilterPosition = getFirstFilterPosition(orderedMap, AuthenticationWebFilter.class);
            Integer oAuth2AuthorizationRequestRedirectWebFilterPosition = getFirstFilterPosition(orderedMap, OAuth2AuthorizationRequestRedirectWebFilter.class);

            int filterPosition = Stream.of(authenticationWebFilterPosition, oAuth2AuthorizationRequestRedirectWebFilterPosition)
                    .mapToInt(v -> v)
                    .filter(v -> v > -1)
                    .min()
                    .orElse(-1);

            assertThat(filterPosition, is(not(equalTo( -1))));

            assertThat(allBodyCachingFilterPositions.getFirst(), is(equalTo(filterPosition - 1)));

        });
    }

    @Test
    void testLogoutFilter() {
        filterChains.forEach(filterChain -> {

            ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
            WebFilterExchange webFilterChain = new WebFilterExchange(exchange, filterExchange -> Mono.empty());

            Map<Integer, Object> orderedMap = getFilterOrder(filterChain);
            LogoutWebFilter filter = getFilter(orderedMap, LogoutWebFilter.class);

            Field privateField;
            try {
                privateField = LogoutWebFilter.class.getDeclaredField("logoutSuccessHandler");
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }

            // Set the accessibility as true
            privateField.setAccessible(true);

            ServerLogoutSuccessHandler handler;
            try {
                handler = (ServerLogoutSuccessHandler) privateField.get(filter);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            handler.onLogoutSuccess(webFilterChain, null).block();

            assertThat(exchange.getResponse().getStatusCode(), is(HttpStatus.FOUND));
            assertThat(exchange.getResponse().getHeaders(), hasKey("Location"));
            assertThat(exchange.getResponse().getHeaders().get("Location"), is(not(nullValue())));
            assertThat(exchange.getResponse().getHeaders().get("Location"), hasSize(1));
            assertThat(exchange.getResponse().getHeaders().get("Location").getFirst(), endsWith(URLEncoder.encode(REDIRECT_ENDPOINT, StandardCharsets.UTF_8)));
            assertThat(exchange.getResponse().getHeaders().get("Location").getFirst(), containsString("?%s=".formatted(REDIRECT_KEY)));

        });
    }

    @Test
    void testLogoutFilterPosition(){

        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

        filterChains.forEach(filterChain -> {
            Map<Integer, Object> orderedMap = getFilterOrder(filterChain);

            if (Boolean.TRUE.equals(filterChain.matches(exchange).block())){

                Integer logoutWebFilterPosition = getFirstFilterPosition(orderedMap, LogoutWebFilter.class);
                assertThat(logoutWebFilterPosition, is(not(equalTo( -1))));

                Integer logoutPageGeneratingWebFilterPosition = getFirstFilterPosition(orderedMap, LogoutPageGeneratingWebFilter.class);
                assertThat(logoutPageGeneratingWebFilterPosition, is(not(equalTo( -1))));

                assertThat(logoutPageGeneratingWebFilterPosition, is(equalTo(logoutWebFilterPosition + 1)));


            } else {

                Integer logoutWebFilterPosition = getFirstFilterPosition(orderedMap, LogoutWebFilter.class);
                assertThat(logoutWebFilterPosition, is(not(equalTo( -1))));

                Integer logoutPageGeneratingWebFilterPosition = getFirstFilterPosition(orderedMap, LogoutPageGeneratingWebFilter.class);
                assertThat(logoutPageGeneratingWebFilterPosition, is(equalTo( -1)));

            }
        });
    }

    private <T> T getFilter(Map<Integer, Object> orderMap, Class<T> filterClass){

        for (Map.Entry<Integer, Object> entry : orderMap.entrySet()) {
            if (filterClass.isInstance(entry.getValue())) {
                return filterClass.cast(entry.getValue());
            }
        }

        throw new IllegalArgumentException("Could not find filter class %s".formatted(filterClass.getSimpleName()));
    }

    private List<Integer> getAllFilterPositions(Map<Integer, Object> orderMap, Class<?> filterClass){

        List<Integer> result = new ArrayList<>();

        for (Map.Entry<Integer, Object> entry : orderMap.entrySet()) {
            if (filterClass.isInstance(entry.getValue())) {
                result.add(entry.getKey());
            }
        }

        Collections.sort(result);

        return result;
    }

    private Integer getFirstFilterPosition(Map<Integer, Object> orderMap, Class<?> filterClass){

        List<Integer> result = getAllFilterPositions(orderMap, filterClass);

        if (result.isEmpty()) {
            return -1;
        }

        return result.getFirst();

    }


    private Map<Integer, Object> getFilterOrder(SecurityWebFilterChain filterChain){

        Map<Integer, Object> result = new HashMap<>();

        List<WebFilter> filters = filterChain.getWebFilters().collectList().block();

        if (filters == null || filters.isEmpty()) {
            return result;
        }

        forEachWithCounter(filters, result::put);

        return result;

    }

    private static <T> void forEachWithCounter(Iterable<T> source, BiConsumer<Integer, T> consumer) {
        int i = 0;
        for (T item : source) {
            consumer.accept(i, item);
            i++;
        }
    }

}