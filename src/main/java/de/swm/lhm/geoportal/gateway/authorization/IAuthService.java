package de.swm.lhm.geoportal.gateway.authorization;

import de.swm.lhm.geoportal.gateway.authorization.model.AuthorizationGroup;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import de.swm.lhm.geoportal.gateway.resource.model.FileResourcePath;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * Interface for authorization service.
 */
public interface IAuthService {

    /**
     * Retrieves the access information grouped for a portal.
     *
     * @param portalName The name of the portal.
     * @return A {@link Mono} emitting the {@link AuthorizationGroup} for the portal.
     */
    Mono<AuthorizationGroup> getAccessInfoGroupForPortal(String portalName);

    /**
     * Retrieves the access information grouped for multiple geoservice layers.
     *
     * @param layerNames The set of geoservice layer names.
     * @return A {@link Mono} emitting a list of {@link AuthorizationGroup}s for the specified layers.
     */
    Mono<List<AuthorizationGroup>> getAccessInfoGroupForGeoServiceLayers(Set<QualifiedLayerName> layerNames);

    /**
     * Retrieves the access information grouped for a specific geoservice layer.
     *
     * @param layerName The name of the geoservice layer.
     * @return A {@link Mono} emitting the {@link AuthorizationGroup} for the layer.
     */
    Mono<AuthorizationGroup> getAccessInfoGroupForGeoServiceLayer(QualifiedLayerName layerName);

    /**
     * Retrieves the access information grouped for a file resource.
     *
     * @param path The file resource path.
     * @return A {@link Mono} emitting the {@link AuthorizationGroup} for the file resource.
     */
    Mono<AuthorizationGroup> getAccessInfoGroupForFileResource(FileResourcePath path);

    /**
     * Retrieves the granted product roles from the authentication.
     *
     * @param authentication The authentication object.
     * @return A set of granted product roles.
     */
    Set<String> getGrantedProducts(Authentication authentication);

    /**
     * Retrieves the authentication object from the security context.
     *
     * @return A {@link Mono} emitting the {@link Authentication} object.
     */
    Mono<Authentication> getAuthentication();

    /**
     * Retrieves the authentication object from the security context associated with the server web exchange.
     *
     * @param exchange The server web exchange.
     * @return A {@link Mono} emitting the {@link Authentication} object.
     */
    Mono<Authentication> getAuthentication(ServerWebExchange exchange);

    /**
     * Asynchronously retrieves the granted product roles.
     *
     * @return A {@link Mono} emitting a set of granted product roles.
     */
    Mono<Set<String>> getGrantedProducts();

    /**
     * Checks if the granted authorization level is high.
     *
     * @param authentication The authentication object.
     * @return {@code true} if the granted authorization level is high, otherwise {@code false}.
     */
    boolean getGrantedAuthLevelHigh(Authentication authentication);

    /**
     * Asynchronously checks if the granted authorization level is high.
     *
     * @return A {@link Mono} emitting {@code true} if the granted authorization level is high, otherwise {@code false}.
     */
    Mono<Boolean> getGrantedAuthLevelHigh();

    /**
     * Retrieves the granted authorization group.
     *
     * @return A {@link Mono} emitting the {@link AuthorizationGroup}.
     */
    Mono<AuthorizationGroup> getGrantedAsAuthorizationGroup();

    /**
     * Retrieves the subject from the authentication.
     *
     * @return A {@link Mono} emitting the subject string.
     */
    Mono<String> getSubject();

    /**
     * Retrieves the ID token value from the authentication.
     *
     * @return A {@link Mono} emitting the ID token value.
     */
    Mono<String> getIdTokenValue();

    /**
     * Retrieves the ID token value from the authentication associated with the web filter exchange.
     *
     * @param exchange The web filter exchange.
     * @return A {@link Mono} emitting the ID token value.
     */
    Mono<String> getIdTokenValue(WebFilterExchange exchange);

    /**
     * Retrieves the ID token value from the authentication associated with the server web exchange.
     *
     * @param exchange The server web exchange.
     * @return A {@link Mono} emitting the ID token value.
     */
    Mono<String> getIdTokenValue(ServerWebExchange exchange);

    /**
     * Retrieves the username from the authentication.
     *
     * @return A {@link Mono} emitting the username.
     */
    Mono<String> getUserName();
}