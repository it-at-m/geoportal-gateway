package de.swm.lhm.geoportal.gateway.geoservice.filter;

import de.swm.lhm.geoportal.gateway.filter.response.BodyModifyingServerHttpResponse;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceInspectorService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequest;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequestType;
import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;
import de.swm.lhm.geoportal.gateway.util.DataBufferUtils;
import de.swm.lhm.geoportal.gateway.util.HttpHeaderUtils;
import de.swm.lhm.geoportal.gateway.util.messagebody.MessageBodyEncodingService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Slf4j
@Component
public class WfsTransactionalLoggingGatewayFilter extends AbstractGeoServiceResponseGatewayFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger("wfstJsonLogger");

    WfsTransactionalLoggingGatewayFilter(GeoServiceInspectorService geoServiceInspectorService, MessageBodyEncodingService messageBodyEncodingService) {
        super(geoServiceInspectorService, messageBodyEncodingService);
    }

    @Override
    Mono<Void> filterGeoService(ServerWebExchange exchange, GatewayFilterChain chain, GeoServiceRequest geoServiceRequest) {
        if (geoServiceRequest.is(ServiceType.WFS, GeoServiceRequestType.TRANSACTION)) {
            return chain.filter(wrapResponse(exchange, geoServiceRequest).mutateServerWebExchange(exchange));
        } else {
            return chain.filter(exchange);
        }
    }

    private BodyModifyingServerHttpResponse wrapResponse(ServerWebExchange exchange, GeoServiceRequest geoServiceRequest) {
        return new BodyModifyingServerHttpResponse(exchange.getResponse(), this.messageBodyEncodingService) {

            private Mono<String> requestBody() {
                ServerHttpRequest request = exchange.getRequest();
                if (HttpHeaderUtils.isContentTypeXmlOrGml(request.getHeaders())) {
                    return DataBufferUtils.copyAsString(Flux.from(request.getBody()));
                } else {
                    return Mono.just("no xml");
                }
            }

            private Map<String, String> loggableHeaders() {
                HttpHeaders httpHeaders = exchange.getRequest().getHeaders();
                Map<String, String> headers = new HashMap<>();
                for (Map.Entry<String, List<String>> headerEntry : httpHeaders.entrySet()) {
                    String headerValue = headerEntry.getValue().getFirst();
                    if (headerEntry.getKey().equalsIgnoreCase("cookie")) {
                        headerValue = "**omitted**";
                    }
                    headers.put(headerEntry.getKey(), headerValue);
                }
                return headers;
            }

            @Override
            protected Mono<DataBuffer> processBody(Mono<DataBuffer> body) {
                // log response body without altering it
                return DataBufferUtils.copyAsByteArray(body)
                        .zipWith(requestBody())
                        .map(bodyTuples -> {
                            String requestBody = bodyTuples.getT2();

                            String responseBody = "no xml";
                            if (HttpHeaderUtils.isContentTypeXmlOrGml(getHeaders())) {
                                responseBody = new String(bodyTuples.getT1(), UTF_8);
                            }

                            // https://github.com/logfellow/logstash-logback-encoder
                            LOGGER.info("WFS-T data mutation",
                                    keyValue("request_headers", loggableHeaders()),
                                    keyValue("remote_host", exchange.getRequest().getRemoteAddress()),
                                    keyValue("layer", geoServiceRequest.getLayers()),
                                    keyValue("request", requestBody),
                                    keyValue("response", responseBody)
                            );

                            return getDelegate().bufferFactory().wrap(bodyTuples.getT1());
                        });
            }

        };
    }
}
