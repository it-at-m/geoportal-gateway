package de.swm.lhm.geoportal.gateway.loadbalancer;

import de.swm.lhm.geoportal.gateway.util.UrlParser;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class StickyLoadBalancerFilter implements GlobalFilter, Ordered {

    final AtomicInteger position;
    private final LoadBalancerReactiveClient reactiveClient;

    public StickyLoadBalancerFilter(
            @NotNull LoadBalancerReactiveClient reactiveClient
    ) {
        this.position = new AtomicInteger(new Random().nextInt(1000));
        this.reactiveClient = reactiveClient;
    }

    @Override
    public int getOrder() {
        return ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER - 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        URI url = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        String schemePrefix = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR);

        if (shouldFilter(url, schemePrefix)) {

            ServerWebExchangeUtils.addOriginalRequestUrl(
                    exchange,
                    exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR)
            );

            String serviceId = UrlParser.resolveHostTolerant(url);
            return this.chooseInstance(exchange, serviceId)
                    .flatMap(serviceInstance -> this.setAttributes(exchange, serviceInstance, url, schemePrefix))
                    .then(chain.filter(exchange));

        }

        return chain.filter(exchange);
    }

    private boolean shouldFilter(URI url, String schemePrefix) {

        return (
                url != null
                        && ("lb".equals(url.getScheme()) || "lb".equals(schemePrefix))
                        && reactiveClient.isSticky(UrlParser.resolveHostTolerant(url))
        );

    }

    private Mono<LoadBalancerServiceInstance> chooseInstance(ServerWebExchange exchange, String serviceId) {

        return exchange.getSession()
                .map(webSession -> reactiveClient.chooseInstance(webSession, serviceId));

    }

    private Mono<Void> setAttributes(ServerWebExchange exchange, LoadBalancerServiceInstance serviceInstance, URI url, String schemePrefix) {

        URI uri = exchange.getRequest().getURI();
        String overrideScheme = serviceInstance.getScheme();

        if (schemePrefix != null) {
            overrideScheme = url.getScheme();
        }

        URI requestUrl = this.reconstructURI(new DelegatingServiceInstance(serviceInstance, overrideScheme), uri);

        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, requestUrl);
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR, new DefaultResponse(serviceInstance));

        log.debug("Chosen url for service {} is {}", serviceInstance.getServiceId(), serviceInstance.getUri());

        return Mono.empty();

    }


    protected URI reconstructURI(ServiceInstance serviceInstance, URI original) {
        return LoadBalancerUriTools.reconstructURI(serviceInstance, original);
    }


}
