package de.swm.lhm.geoportal.gateway.portal;

import de.swm.lhm.geoportal.gateway.portal.model.Portal;
import de.swm.lhm.geoportal.gateway.product.HasProductDetails;
import de.swm.lhm.geoportal.gateway.product.model.Product;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import de.swm.lhm.geoportal.gateway.unit.UnitService;
import de.swm.lhm.geoportal.gateway.util.ExtendedURIBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URISyntaxException;
import java.util.List;

@Service
@Getter
@Setter
@RequiredArgsConstructor
@Slf4j
public class PortalService implements HasProductDetails {

    private final PortalProperties portalProperties;
    private final PortalRepository portalRepository;
    private final UnitService unitService;
    private final GatewayService gatewayService;

    public String extractPortalName(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        PathContainer requestPath = request.getPath().pathWithinApplication();

        List<String> pathList = requestPath.elements()
                .stream()
                .filter(element -> element instanceof PathContainer.PathSegment)
                .map(PathContainer.Element::value)
                .toList();

        String ident = "portal";
        int index = 1;
        if (requestPath.value().startsWith(this.portalProperties.getApiEndpoint())) {
            ident = "portal api";
            index = 2;
        }
        String portalName = pathList.get(index);

        log.debug("Identified a {} url: {}, portal: {}", ident, request.getURI(), portalName);
        return portalName;
    }

    public boolean isPortalRequest(ServerWebExchange exchange) {
        String requestPath = exchange.getRequest().getPath().value();
        return requestPath.startsWith(this.portalProperties.getEndpoint())
                || requestPath.startsWith(this.portalProperties.getApiEndpoint());
    }

    @Override
    public Mono<Void> enrichProduct(Product product) {
        return portalRepository.findPortalByProductId(product.getId(), product.getStage())
                .flatMap(this::enrichPortal)
                .doOnNext(product::addPortal)
                .then();
    }

    private Mono<Portal> enrichPortal(Portal portal) {
        return unitService.getUnitNameById(portal.getUnitId())
                .doOnNext(portal::setUnit)
                .thenReturn(portal)
                .doOnNext(this::mapPortalUrl)
                .thenReturn(portal);
    }

    private void mapPortalUrl(Portal portal) {
        portal.setUrl(buildPortalUrl(portal));
    }

    private String buildPortalUrl(Portal portal) {
        try {
            return new ExtendedURIBuilder(gatewayService.getExternalUrl())
                    .addPath(portalProperties.getEndpoint())
                    .addPath(portal.getName()).toString();
        } catch (URISyntaxException e) {
            log.atError()
               .setMessage(() -> String.format("Failed to build url for portal %s", portal.getName()))
               .setCause(e)
               .log();
            return "";
        }
    }
}