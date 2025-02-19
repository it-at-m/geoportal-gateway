package de.swm.lhm.geoportal.gateway.geoservice;

import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceInspectorService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceRequestType;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import de.swm.lhm.geoportal.gateway.geoservice.model.GeoService;
import de.swm.lhm.geoportal.gateway.product.HasProductDetails;
import de.swm.lhm.geoportal.gateway.product.model.Product;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;
import de.swm.lhm.geoportal.gateway.util.ExtendedURIBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URISyntaxException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeoServicesService implements HasProductDetails {

    private final GeoServiceRepository geoServiceRepository;
    private final GeoServiceInspectorService geoServiceInspectorService;
    private final GatewayService gatewayService;

    @Override
    public Mono<Void> enrichProduct(Product product) {
        return geoServiceRepository.findGeoServiceByProductId(product.getId(), product.getStage())
                .map(this::mapGeoServiceToProductService)
                .doOnNext(product::addGeoService)
                .then();
    }

    public Mono<Boolean> layerHasWfsTEnabled(QualifiedLayerName qualifiedLayerName) {
        return geoServiceRepository.layerHasWfsTEnabled(gatewayService.getStage(), qualifiedLayerName);
    }

    private Product.Service mapGeoServiceToProductService(GeoService geoService) {
        return Product.Service.builder()
                .name(geoService.getName())
                .workspace(geoService.getWorkspace())
                .accessLevel(geoService.getAccessLevel())
                .authLevelHigh(geoService.getAuthLevelHigh())
                .urls(buildUrls(geoService))
                .build();
    }

    private List<Product.Service.ServiceUrl> buildUrls(GeoService geoService) {
        return geoService.getServiceTypes().stream()
                .filter(geoServiceType -> geoServiceType != ServiceType.WMTS)
                .map(geoServiceType -> buildUrl(geoService, geoServiceType))
                .toList();
    }

    private Product.Service.ServiceUrl buildUrl(GeoService geoService, ServiceType serviceType) {
        return Product.Service.ServiceUrl.builder()
                .serviceType(serviceType)
                .url(buildUrlString(geoService, serviceType))
                .build();
    }

    private String buildUrlString(GeoService geoService, ServiceType serviceType) {
        try {
            return new ExtendedURIBuilder(gatewayService.getExternalUrl())
                    .addPath(geoServiceInspectorService.getGeoServicePathPrefix())
                    .addPath(geoService.getWorkspace())
                    .addPath(geoService.getName())
                    .addPath("ows")
                    .addParameter("request", GeoServiceRequestType.GET_CAPABILITIES.name)
                    .addParameter("service", serviceType.name())
                    .addParameter("version", serviceType.getVersion())
                    .toString();

        } catch (URISyntaxException e) {
            log.atError()
               .setMessage(() -> String.format("Failed to build service url for service %s and service type %s", geoService, serviceType))
               .setCause(e)
               .log();
            return "";
        }
    }
}