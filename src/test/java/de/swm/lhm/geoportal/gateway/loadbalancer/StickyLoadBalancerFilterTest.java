package de.swm.lhm.geoportal.gateway.loadbalancer;

import de.swm.lhm.geoportal.gateway.base_classes.config.TestSessionConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.mock.web.server.MockWebSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;


// https://github.com/spring-cloud/spring-cloud-gateway/blob/main/spring-cloud-gateway-server
// /src/test/java/org/springframework/cloud/gateway/filter/ReactiveLoadBalancerClientFilterTests.java


@ExtendWith(SpringExtension.class)
@Import(TestSessionConfig.class)
@EnableConfigurationProperties(value = LoadBalancerProperties.class)
@TestPropertySource("classpath:loadbalancer/load-balancer-properties-test.properties")
class StickyLoadBalancerFilterTest {

    private final GatewayFilterChain chain = mock(GatewayFilterChain.class);
    @Autowired
    private LoadBalancerProperties loadBalancerProperties;
    private LoadBalancerReactiveClient client;
    private StickyLoadBalancerFilter filter;

    @BeforeEach
    void setUp() {
        client = new LoadBalancerReactiveClient(loadBalancerProperties);
        filter = new StickyLoadBalancerFilter(client);
    }

    @Test
    void shouldFilterTest() throws URISyntaxException {

        ServerWebExchange exchange;
        ServerWebExchange filteredExchange;

        URI url = new URI("lb://test__1__");
        MockWebSession session = new MockWebSession();

        exchange = createExchange(session, url, "/mypath");
        filteredExchange = executeFilter(exchange, 1);

        URI firstUrl = filteredExchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        DefaultResponse firstResponse = filteredExchange.getAttribute(GATEWAY_LOADBALANCER_RESPONSE_ATTR);

        exchange = createExchange(session, url, "/mypath");
        filteredExchange = executeFilter(exchange, 2);

        URI secondUrl = filteredExchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        DefaultResponse secondResponse = filteredExchange.getAttribute(GATEWAY_LOADBALANCER_RESPONSE_ATTR);

        assertThat(firstResponse, is(secondResponse));
        assertThat(firstUrl, is(secondUrl));

    }

    private ServerWebExchange createExchange(MockWebSession session, URI url, String path){
        MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
        ServerWebExchange exchange = MockServerWebExchange.builder(request).session(session).build();
        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, url);
        return exchange;
    }

    private ServerWebExchange executeFilter(ServerWebExchange exchange, int callTimes) {

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());
        filter.filter(exchange, chain).block();
        verify(chain, times(callTimes)).filter(any(ServerWebExchange.class));
        verifyNoMoreInteractions(chain);
        return captor.getValue();

    }

    @Test
    void shouldNotFilterTest() throws URISyntaxException {

        URI url = new URI("http://localhost:8083");

        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/mypath").build());
        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, url);
        ServerWebExchange filteredExchange = executeFilter(exchange, 1);

        URI resultUrl = filteredExchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        assertThat(url, is(resultUrl));

        DefaultResponse loadBalancerResponse = filteredExchange.getAttribute(GATEWAY_LOADBALANCER_RESPONSE_ATTR);
        assertThat(loadBalancerResponse, nullValue());

        HttpHeaders responseHeaders = filteredExchange.getResponse().getHeaders();
        assertThat(responseHeaders.isEmpty(), is(Boolean.TRUE));

        String setCookieHeader = responseHeaders.getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader, nullValue());
    }

    @Test
    void filterOrderTest() {

        assertThat(filter.getOrder(), is(ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER - 1));

    }

    @Test
    void clientTest() {

        List<String> services = client.getServices().collectList().block();

        assertThat(services, containsInAnyOrder("test__1__", "test__2__", "test__3__", "test__4__", "test__5__", "test__6__"));

        List<ServiceInstance> instances = client.getInstances("test__1__").collectList().block();

        assertThat(instances, notNullValue());

        List<String> instancesList = new ArrayList<>();
        for (ServiceInstance instance : instances) {
            instancesList.add(instance.getUri().toString());
        }

        assertThat(instancesList, containsInAnyOrder("http://localhost:8083/", "http://localhost:8084/"));

        // increase code coverage

        client.description();
        client.getOrder();
        client.reactiveProbe();

    }

}
