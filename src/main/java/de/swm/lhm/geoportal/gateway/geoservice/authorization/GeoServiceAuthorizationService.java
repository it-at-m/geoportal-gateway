package de.swm.lhm.geoportal.gateway.geoservice.authorization;

import de.swm.lhm.geoportal.gateway.authorization.IAuthService;
import de.swm.lhm.geoportal.gateway.geoservice.GeoServiceRepository;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class GeoServiceAuthorizationService {
    private final IAuthService authorizationService;
    private final GatewayService gatewayService;
    private final GeoServiceRepository geoServiceRepository;

    public Mono<Set<QualifiedLayerName>> getNonHiddenGeoServiceLayersLowercased() {
        return authorizationService.getGrantedAsAuthorizationGroup()
                .flatMapMany(authorizationGroup -> geoServiceRepository.findNonHiddenGeoServiceLayers(gatewayService.getStage(), authorizationGroup))
                .map(String::toLowerCase) // geoserver layer names are case-insensitive
                .map(QualifiedLayerName::fromString)
                .collectList()
                .map(Set::copyOf);

    }
}
