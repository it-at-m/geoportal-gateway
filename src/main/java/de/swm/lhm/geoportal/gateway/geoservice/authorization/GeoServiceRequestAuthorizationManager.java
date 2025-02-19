package de.swm.lhm.geoportal.gateway.geoservice.authorization;

import de.swm.lhm.geoportal.gateway.authorization.IAuthService;
import de.swm.lhm.geoportal.gateway.authorization.model.AuthorizationGroup;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.GeoServiceInspectorService;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Setter
@Getter
@Slf4j
@RequiredArgsConstructor
public class GeoServiceRequestAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final IAuthService authorizationService;
    private final GeoServiceInspectorService geoServiceInspectorService;
    private final AuthenticationTrustResolver authTrustResolver = new AuthenticationTrustResolverImpl();

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authenticationMono, AuthorizationContext authorizationContext) {
        return withGeoServiceAuthorizationContext(
                authorizationContext,
                geoServiceAuthorizationContext ->
                        authenticationMono.flatMap(authentication -> {
                                    // authentication is not empty. user is logged in, so granted products are loaded and get checked

                                    Set<String> grantedProducts = authorizationService.getGrantedProducts(authentication);
                                    boolean userHasAuthLevelHigh = authorizationService.getGrantedAuthLevelHigh(authentication);

                                    return getAuthorizationDecision(
                                            geoServiceAuthorizationContext,
                                            authorizationGroup -> AuthorizationGroup.isAuthorized(authorizationGroup, grantedProducts, userHasAuthLevelHigh)
                                    );

                                })
                                .switchIfEmpty(
                                        // unauthenticated, so no authorization was supplied, any product-based authorization checks are skipped
                                        getAuthorizationDecision(
                                                geoServiceAuthorizationContext,
                                                authorizationGroup -> new AuthorizationDecision(authorizationGroup.isPublic())))

        )
                // restrict access unless allowed by any former branch
                .switchIfEmpty(Mono.just(new AuthorizationDecision(false)));
    }

    private Mono<AuthorizationDecision> withGeoServiceAuthorizationContext(AuthorizationContext authorizationContext, Function<GeoServiceAuthorizationContext, Mono<AuthorizationDecision>> inner) {
        Object gsAuthContextObj = authorizationContext.getVariables().getOrDefault(
                GeoServiceAuthorizationContext.MATCHRESULT_KEY,
                new GeoServiceAuthorizationContext()
        );

        if (gsAuthContextObj instanceof GeoServiceAuthorizationContext gsAuthContext) {

            if (!gsAuthContext.isSupportedRequest()) {
                return Mono.just(new AuthorizationDecision(false));
            }

            return inner.apply(gsAuthContext);
        } else {
            log.error("Expected a GeoServiceAuthorizationContext, received an object of class {}", gsAuthContextObj.getClass().getSimpleName());
            return Mono.just(new AuthorizationDecision(false));
        }
    }

    private Mono<AuthorizationDecision> getAuthorizationDecision(GeoServiceAuthorizationContext geoServiceAuthorizationContext, Function<AuthorizationGroup, AuthorizationDecision> authorizationDecisionFunction) {

        Mono<Set<QualifiedLayerName>> stylePreviewAllowedLayers = geoServiceAuthorizationContext
                .getStylePreview()
                .map(stylePreview -> new QualifiedLayerName(stylePreview.getLayerWorkspace(), stylePreview.getLayerName()))
                .collect(Collectors.toSet());

        // there may be multiple layers within one request, so this expects a list of authorization groups
        // An example for a request referencing multiple layers would be a WMS GetMap-request with the
        // query parameter "layers=layer1,layer2,layer3".
        //
        // Assume access is granted unless proven otherwise. This is also required as public
        // layers will not be contained in the access-infos list
        return Flux.fromStream(
                        geoServiceAuthorizationContext
                                .getLayerAuthorizationGroups()
                                .entrySet()
                                .stream()
                )
                .flatMap(entry -> stylePreviewAllowedLayers
                        .map(allowedLayers -> {
                            if (allowedLayers.contains(entry.getKey())) {
                                // granted by the style-preview token attached to the request
                                return new AuthorizationDecision(true);
                            } else {
                                return authorizationDecisionFunction.apply(entry.getValue());
                            }
                        })
                        .switchIfEmpty(
                                Mono.fromCallable(() -> authorizationDecisionFunction.apply(entry.getValue()))
                        )
                )
                .all(AuthorizationDecision::isGranted)
                .defaultIfEmpty(false)
                .map(AuthorizationDecision::new);
    }
}
