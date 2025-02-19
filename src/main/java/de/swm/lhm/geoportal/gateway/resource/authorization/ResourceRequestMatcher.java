package de.swm.lhm.geoportal.gateway.resource.authorization;

import de.swm.lhm.geoportal.gateway.authorization.IAuthService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import de.swm.lhm.geoportal.gateway.resource.ResourceProperties;
import de.swm.lhm.geoportal.gateway.resource.ResourceRepository;
import de.swm.lhm.geoportal.gateway.resource.model.FileResourcePath;
import de.swm.lhm.geoportal.gateway.resource.model.FileResourcePathFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@Component
@Setter
@Getter
@Slf4j
@RequiredArgsConstructor
public class ResourceRequestMatcher implements ServerWebExchangeMatcher {
    private final ResourceProperties resourceProperties;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ResourceRepository resourceRepository;
    private final FileResourcePathFactory fileResourcePathFactory;
    private final IAuthService authorizationService;

    @Override
    public Mono<MatchResult> matches(ServerWebExchange exchange) {
        String requestPath = exchange.getRequest().getPath().value();
        if (!isResourceRequest(requestPath)) {
            return MatchResult.notMatch();
        }

        Optional<FileResourcePath> fileResourcePathOptional = fileResourcePathFactory.createFileResourcePath(requestPath);

        if (fileResourcePathOptional.isEmpty()) {
            log.warn("Resource request will not be filtered as FileResourcePath can't be created");
            return MatchResult.notMatch();
        }

        FileResourcePath fileResourcePath = fileResourcePathOptional.get();
        Optional<QualifiedLayerName> layerName = fileResourcePath.extractQualifiedLayerName();

        /*
        check if the resource is protected via configuration (via linking it to a product or portal or by directly setting the auth level)
        or if the resource belongs to a geoserver layer and if this layer is protected
        */
        return authorizationService.getAccessInfoGroupForFileResource(fileResourcePath)
                .flatMap(configuredAccess -> {

                    if (layerName.isEmpty()) {
                        if (configuredAccess.isPublic()) {
                            log.debug("Identified fileResource '{}': no layer found and fileResource is public, no filtering applied", fileResourcePath);
                            return MatchResult.notMatch();
                        } else {
                            log.debug("Identified fileResource '{}': no layer found and fileResource is not public, filtering applied", fileResourcePath);
                            return MatchResult.match(
                                    Map.of(ResourceRequestAuthorizationContext.KEY,
                                            ResourceRequestAuthorizationContext.builder()
                                                    .fileResourcePath(fileResourcePath)
                                                    .configuredAuthorizationGroup(configuredAccess)
                                                    .build()
                                    )
                            );
                        }

                    } else {
                        log.debug("Identified fileResource '{}' belongs to the geoserver layer {}", fileResourcePath, layerName.get());

                        return authorizationService.getAccessInfoGroupForGeoServiceLayer(layerName.get())
                                .flatMap(
                                        layerAccess -> {
                                            if (configuredAccess.isPublic() && layerAccess.isPublic()) {
                                                log.debug("Identified fileResource '{}': layer {} is public and fileResource is public, no filtering applied", fileResourcePath, layerName.get());
                                                return MatchResult.notMatch();
                                            } else {
                                                log.debug("Identified fileResource '{}': layer {} or fileResource is not public, filtering applied", fileResourcePath, layerName.get());
                                                return MatchResult.match(
                                                        Map.of(ResourceRequestAuthorizationContext.KEY,
                                                                ResourceRequestAuthorizationContext.builder()
                                                                        .fileResourcePath(fileResourcePath)
                                                                        .configuredAuthorizationGroup(configuredAccess)
                                                                        .layerAuthorizationGroup(layerAccess)
                                                                        .build()));
                                            }
                                        });

                    }
                });
    }


    private boolean isResourceRequest(String requestPath) {
        return requestPath.startsWith(resourceProperties.getEndpoint());
    }




}


