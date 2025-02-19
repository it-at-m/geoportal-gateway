package de.swm.lhm.geoportal.gateway.geoservice.filter;

import de.swm.lhm.geoportal.gateway.filter.gatewayfilter.FilterOrder;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceInspectorService;
import de.swm.lhm.geoportal.gateway.util.messagebody.MessageBodyEncodingService;
import org.springframework.core.Ordered;

abstract class AbstractGeoServiceResponseGatewayFilter extends AbstractGeoServiceGatewayFilter implements Ordered {
    protected final MessageBodyEncodingService messageBodyEncodingService;

    AbstractGeoServiceResponseGatewayFilter(GeoServiceInspectorService geoServiceInspectorService, MessageBodyEncodingService messageBodyEncodingService) {
        super(geoServiceInspectorService);
        this.messageBodyEncodingService = messageBodyEncodingService;
    }

    @Override
    public int getOrder() {
        return FilterOrder.RESPONSE_BEFORE_NETTY_WRITE_FILTER.getOrder();
    }

}
