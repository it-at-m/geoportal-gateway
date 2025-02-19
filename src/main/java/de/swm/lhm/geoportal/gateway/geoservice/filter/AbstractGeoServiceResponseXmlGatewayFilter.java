package de.swm.lhm.geoportal.gateway.geoservice.filter;

import de.swm.lhm.geoportal.gateway.geoservice.filter.response.XmlBodyModifyingServerHttpResponse;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceInspectorService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequest;
import de.swm.lhm.geoportal.gateway.util.messagebody.MessageBodyEncodingService;
import de.swm.lhm.geoportal.gateway.util.XmlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import org.w3c.dom.Document;
import reactor.core.publisher.Mono;

import javax.xml.parsers.DocumentBuilderFactory;

@Slf4j
abstract class AbstractGeoServiceResponseXmlGatewayFilter extends AbstractGeoServiceResponseGatewayFilter {

    final DocumentBuilderFactory documentBuilderFactory = XmlUtils.getSecuredDocumentBuilderFactory();

    AbstractGeoServiceResponseXmlGatewayFilter(GeoServiceInspectorService geoServiceInspectorService, MessageBodyEncodingService messageBodyEncodingService) {
        super(geoServiceInspectorService, messageBodyEncodingService);
    }

    protected abstract Boolean isApplicable(GeoServiceRequest geoServiceRequest);

    protected abstract Mono<Document> rewriteDocument(Document document, GeoServiceRequest geoServiceRequest);

    @Override
    Mono<Void> filterGeoService(ServerWebExchange exchange, GatewayFilterChain chain, GeoServiceRequest geoServiceRequest) {
        if (Boolean.TRUE.equals(this.isApplicable(geoServiceRequest))) {

            XmlBodyModifyingServerHttpResponse responseWrapper = new XmlBodyModifyingServerHttpResponse(
                    exchange.getResponse(),
                    documentBuilderFactory,
                    geoServiceInspectorService.getGeoServiceProperties(),
                    this.messageBodyEncodingService) {
                @Override
                protected Mono<Document> processBodyDocument(Document document) {
                    return rewriteDocument(document, geoServiceRequest);
                }
            };

            return chain.filter(responseWrapper.mutateServerWebExchange(exchange));
        } else {
            return chain.filter(exchange);
        }
    }
}
