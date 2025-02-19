package de.swm.lhm.geoportal.gateway.geoservice.filter;

import de.swm.lhm.geoportal.gateway.filter.response.BodyModifyingServerHttpResponse;
import de.swm.lhm.geoportal.gateway.geoservice.HostReplacer;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceInspectorService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequest;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequestType;
import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;
import de.swm.lhm.geoportal.gateway.util.DataBufferUtils;
import de.swm.lhm.geoportal.gateway.util.HttpHeaderUtils;
import de.swm.lhm.geoportal.gateway.util.messagebody.MessageBodyEncodingService;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class HostnameReplacingGatewayFilter extends AbstractGeoServiceResponseGatewayFilter {
    final HostReplacer hostReplacer;

    HostnameReplacingGatewayFilter(GeoServiceInspectorService geoServiceInspectorService, HostReplacer hostReplacer,
            MessageBodyEncodingService messageBodyEncodingService) {
        super(geoServiceInspectorService, messageBodyEncodingService);
        this.hostReplacer = hostReplacer;
    }

    @Override
    Mono<Void> filterGeoService(ServerWebExchange exchange, GatewayFilterChain chain, GeoServiceRequest geoServiceRequest) {
        if (isApplicable(geoServiceRequest)) {

            BodyModifyingServerHttpResponse responseWrapper = new BodyModifyingServerHttpResponse(exchange.getResponse(), this.messageBodyEncodingService) {

                @Override
                public Mono<Boolean> supportsBodyRewrite() {
                    return Mono.just(HttpHeaderUtils.isTextFormatContentType(getHeaders()));
                }

                @Override
                protected Mono<DataBuffer> processBody(Mono<DataBuffer> body) {
                    return DataBufferUtils.copyAsString(body)
                            .map(hostReplacer::replaceInternalHostNames)
                            .map(xmlString -> getDelegate().bufferFactory().wrap(xmlString.getBytes(UTF_8)));
                }
            };

            return chain.filter(responseWrapper.mutateServerWebExchange(exchange));
        } else {
            return chain.filter(exchange);
        }
    }

    private boolean isApplicable(GeoServiceRequest geoServiceRequest) {
        return (geoServiceRequest.is(GeoServiceRequestType.GET_CAPABILITIES)
                || geoServiceRequest.is(ServiceType.WFS, GeoServiceRequestType.DESCRIBE_FEATURE_TYPE));
    }
}
