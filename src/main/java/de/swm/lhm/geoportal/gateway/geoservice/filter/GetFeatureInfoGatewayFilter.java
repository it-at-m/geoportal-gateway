package de.swm.lhm.geoportal.gateway.geoservice.filter;

import de.swm.lhm.geoportal.gateway.filter.response.BodyModifyingServerHttpResponse;
import de.swm.lhm.geoportal.gateway.geoservice.gfi.GfiFilter;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceInspectorService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequest;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequestType;
import de.swm.lhm.geoportal.gateway.util.DataBufferUtils;
import de.swm.lhm.geoportal.gateway.util.messagebody.MessageBodyEncodingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@Slf4j
public class GetFeatureInfoGatewayFilter extends AbstractGeoServiceResponseGatewayFilter {
    public static final String MESSAGE_UNSUPPORTED_FORMAT = "Unsupported GetFeatureInfo format";
    public static final String MESSAGE_NO_CONTENT_TYPE = "GetFeatureInfo response contained no content-type";

    private final List<GfiFilter> gfiFilters;

    GetFeatureInfoGatewayFilter(GeoServiceInspectorService geoServiceInspectorService, List<GfiFilter> gfiFilters,
            MessageBodyEncodingService messageBodyEncodingService) {
        super(geoServiceInspectorService, messageBodyEncodingService);
        this.gfiFilters = gfiFilters;
    }

    @Override
    Mono<Void> filterGeoService(ServerWebExchange exchange, GatewayFilterChain chain, GeoServiceRequest geoServiceRequest) {
        if (isApplicable(geoServiceRequest)) {
            return chain.filter(wrapResponse(exchange, geoServiceRequest).mutateServerWebExchange(exchange));
        } else {
            return chain.filter(exchange);
        }
    }

    private BodyModifyingServerHttpResponse wrapResponse(ServerWebExchange exchange, GeoServiceRequest geoServiceRequest) {
        return new BodyModifyingServerHttpResponse(exchange.getResponse(), this.messageBodyEncodingService) {
            private Mono<DataBuffer> returnInternalServerError(String message) {
                this.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                this.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
                return Mono.fromCallable(() -> getDelegate().bufferFactory().wrap(message.getBytes(UTF_8)));

            }

            @Override
            protected Mono<DataBuffer> processBody(Mono<DataBuffer> body) {
                String responseContentType = this.getDelegate().getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);

                if (responseContentType == null) {
                    log.error("Geoserver returned no content type with GetFeatureInfo response");
                    return returnInternalServerError(MESSAGE_NO_CONTENT_TYPE);
                }

                for (GfiFilter gfiFilter : gfiFilters) {
                    if (gfiFilter.supportsFormat(responseContentType)) {
                        return DataBufferUtils.copyAsString(body)
                                .flatMap(responseBodyString -> gfiFilter.filterGetFeatureInfoBody(geoServiceRequest, responseBodyString))
                                .map(filteredResponseBodyString -> getDelegate().bufferFactory().wrap(filteredResponseBodyString.getBytes(UTF_8)));
                    }
                }

                log.error("Geoserver returned a content type with GetFeatureInfo ({}) without a matching GfiFilter available", responseContentType);
                return returnInternalServerError(MESSAGE_UNSUPPORTED_FORMAT);
            }
        };
    }

    private boolean isApplicable(GeoServiceRequest geoServiceRequest) {
        return geoServiceRequest.is(GeoServiceRequestType.GET_FEATURE_INFO);
    }
}
