package de.swm.lhm.geoportal.gateway.authorization;

import de.swm.lhm.geoportal.gateway.authorization.model.AuthorizationGroup;
import de.swm.lhm.geoportal.gateway.authorization.model.AuthorizationInfo;
import de.swm.lhm.geoportal.gateway.authorization.model.StorkQaaLevel;
import de.swm.lhm.geoportal.gateway.authorization.repository.AuthorizationInfoRepository;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import de.swm.lhm.geoportal.gateway.resource.model.FileResourcePath;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import de.swm.lhm.geoportal.gateway.util.ReactiveCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.swm.lhm.geoportal.gateway.authentication.TokenConstants.AUTHORITIES;
import static de.swm.lhm.geoportal.gateway.authentication.TokenConstants.AUTH_LEVEL;
import static de.swm.lhm.geoportal.gateway.authentication.TokenConstants.SUBJECT;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

/**
 * Service for managing authorization information and security context.
 */
@Service
@Slf4j
public class AuthorizationService implements IAuthService {

    private final AuthorizationInfoRepository authorizationInfoRepository;
    private final GatewayService gatewayService;
    private final ReactiveCache<String, AuthorizationGroup> accessInfoListCache;

    /**
     * Constructs an AuthorizationService.
     *
     * @param authorizationInfoRepository The repository for authorization information.
     * @param gatewayService The gateway service used for retrieving stage information and other functionality.
     */
    public AuthorizationService(AuthorizationInfoRepository authorizationInfoRepository, GatewayService gatewayService) {
        this.authorizationInfoRepository = authorizationInfoRepository;
        this.gatewayService = gatewayService;
        this.accessInfoListCache = new ReactiveCache<>(10000, Duration.ofSeconds(10));
    }

    /**
     * Generates a unique cache key based on a portal name.
     *
     * @param portalName The name of the portal.
     * @return The generated cache key for the portal.
     */
    private String portalCacheKey(String portalName) {
        return "portal %s".formatted(portalName);
    }

    /**
     * Generates a unique cache key based on a geoservice name.
     *
     * @param layerName The name of the geoservice layer.
     * @return The generated cache key for the geoservice layer.
     */
    private String geoServiceCacheKey(QualifiedLayerName layerName) {
        return "geoservice %s".formatted(layerName);
    }

    /**
     * Generates a unique cache key based on a file resource path.
     *
     * @param path The file resource path.
     * @return The generated cache key for the file resource.
     */
    private String fileResourceCacheKey(FileResourcePath path) {
        return "fileResource with unit %s and path %s".formatted(path.getUnit(), path.getFilePathWithinDocumentsFolder());
    }

    @Override
    public Mono<AuthorizationGroup> getAccessInfoGroupForPortal(String portalName) {
        return getAccessInfoGroupUsingSupplier(
                portalCacheKey(portalName),
                () -> authorizationInfoRepository.findAuthorizationInfoByResourceIdAndStage(portalName, gatewayService.getStage())
        );
    }

    @Override
    public Mono<List<AuthorizationGroup>> getAccessInfoGroupForGeoServiceLayers(Set<QualifiedLayerName> layerNames) {
        return Flux.fromIterable(layerNames)
                .flatMap(this::getAccessInfoGroupForGeoServiceLayer)
                .collectList();
    }

    @Override
    public Mono<AuthorizationGroup> getAccessInfoGroupForGeoServiceLayer(QualifiedLayerName layerName) {
        return getAccessInfoGroupUsingSupplier(
                geoServiceCacheKey(layerName),
                () -> authorizationInfoRepository.findAuthorizationInfoByGeoServiceLayerAndStage(layerName.toString(), gatewayService.getStage())
        );
    }

    @Override
    public Mono<AuthorizationGroup> getAccessInfoGroupForFileResource(FileResourcePath path) {
        return getAccessInfoGroupUsingSupplier(
                fileResourceCacheKey(path),
                () -> authorizationInfoRepository.findAuthorizationInfoForFileResource(path.getUnit(), path.getFilePathWithinDocumentsFolder(), gatewayService.getStage())
        );
    }

    /**
     * Gets the authorization group using provided supplier, applying caching mechanism.
     *
     * @param cacheKey The cache key.
     * @param authorizationInfoSupplier The supplier providing the authorization info.
     * @return A {@link Mono} emitting the {@link AuthorizationGroup}.
     */
    private Mono<AuthorizationGroup> getAccessInfoGroupUsingSupplier(String cacheKey, Supplier<Flux<AuthorizationInfo>> authorizationInfoSupplier) {
        return accessInfoListCache.get(cacheKey,
                authorizationInfoSupplier.get()
                        .collectList()
                        .map(AuthorizationGroup::fromAccessInfos)
        );
    }

    @Override
    public Mono<Authentication> getAuthentication() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .onErrorResume(NullPointerException.class, e -> {
                    log.error("NullPointerException in getAuthentication", e);
                    return Mono.empty();
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("getAuthentication returned empty result");
                    return Mono.empty();
                }));
    }

    @Override
    public Mono<Authentication> getAuthentication(ServerWebExchange exchange) {
        return Mono.defer(() -> {
            try {
                return exchange.getSession()
                        .mapNotNull(webSession -> webSession.getAttribute(SPRING_SECURITY_CONTEXT_KEY))
                        .filter(SecurityContext.class::isInstance)
                        .cast(SecurityContext.class)
                        .map(SecurityContext::getAuthentication)
                        .switchIfEmpty(Mono.defer(() -> {
                            // may happen when websession does not have the attribute
                            log.debug("getAuthentication(ServerWebExchange) returned empty result");
                            return Mono.empty();
                        }));
            } catch (NullPointerException e) {
                log.error("NullPointerException in getAuthentication(ServerWebExchange)", e);
                return Mono.empty();
            }
        });
    }

    @Override
    public Set<String> getGrantedProducts(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2AuthenticatedPrincipal principal) {
            // Keycloak stores the granted products in an attribute, not in principal.getAttributes()
            Object authorities = principal.getAttributes().get(AUTHORITIES);
            if (authorities instanceof Collection<?> authList) {
                return handleAuthorities(authList);
            }
            return Collections.emptySet();
        }
        // Basic auth authorities are stored directly within the principal
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null || authorities.isEmpty()) {
            return Collections.emptySet();
        }
        return handleAuthorities(authorities);
    }

    /**
     * Handles converting a collection of authorities to a set of authority names.
     *
     * @param authorities The collection of authorities.
     * @return A set of authority names.
     */
    private Set<String> handleAuthorities(Collection<?> authorities) {
        return authorities.stream()
                .flatMap(auth -> {
                    if (auth instanceof String strAuth) {
                        return Stream.of(strAuth);
                    } else if (auth instanceof GrantedAuthority grantedAuth) {
                        return Stream.of(grantedAuth.getAuthority());
                    }
                    return Stream.empty();
                })
                .filter(authorityName -> !StringUtils.isBlank(authorityName))
                .collect(Collectors.toSet());
    }

    @Override
    public Mono<Set<String>> getGrantedProducts() {
        return getAuthentication()
                .map(this::getGrantedProducts)
                .switchIfEmpty(Mono.just(Set.of()))
                .onErrorResume(NullPointerException.class, e -> {
                    log.error("NullPointerException in getGrantedProducts", e);
                    return Mono.just(Set.of());
                });
    }

    @Override
    public boolean getGrantedAuthLevelHigh(Authentication authentication) {
        /*
        Schlüssel__________Vertrauensniveau__Beschreibung
        STORK-QAA-Level-1__niedrig___________Benutzername/Passwort
        STORK-QAA-Level-3__substanziell______ELSTER-Zertifikat
        STORK-QAA-Level-4__hoch______________Online-Ausweisfunktion (Elektronischer Personalausweis, EU-Karte, …)
        */

        StorkQaaLevel authLevel = StorkQaaLevel.fromValue(
                getOAuth2Attribute(
                        authentication,
                        AUTH_LEVEL,
                        StorkQaaLevel.STORK_QAA_LEVEL_1.getValue()
                )
        );

        return authLevel.equals(StorkQaaLevel.STORK_QAA_LEVEL_3) || authLevel.equals(StorkQaaLevel.STORK_QAA_LEVEL_4);
    }

    @Override
    public Mono<Boolean> getGrantedAuthLevelHigh() {
        return getAuthentication()
                .map(this::getGrantedAuthLevelHigh)
                .switchIfEmpty(Mono.just(false))
                .onErrorResume(NullPointerException.class, e -> {
                    log.error("NullPointerException in getGrantedAuthLevelHigh", e);
                    return Mono.just(false);
                });
    }

    @Override
    public Mono<AuthorizationGroup> getGrantedAsAuthorizationGroup() {
        return Mono.zip(
                        getGrantedAuthLevelHigh().switchIfEmpty(Mono.just(false)),
                        getGrantedProducts().switchIfEmpty(Mono.just(Set.of())),
                        getPrincipal().switchIfEmpty(Mono.just(Optional.empty()))
                )
                .map(zipped -> AuthorizationGroup.builder()
                        .authLevelHigh(zipped.getT1())
                        .productRoles(zipped.getT2())
                        .hasPrincipal(zipped.getT3().isPresent())
                        .build()
                )
                .switchIfEmpty(Mono.just(AuthorizationGroup.empty()))
                .onErrorResume(NullPointerException.class, e -> {
                    log.error("NullPointerException in getGrantedAsAuthorizationGroup", e);
                    return Mono.empty();
                });
    }

    @Override
    public Mono<String> getSubject(){
        return getSubjectFromClaimAccessor()
                .doOnError(e -> log.error("Failed to getSubjectFromClaimAccessor", e))
                .switchIfEmpty(getSubjectFromOAuth2Attribute());
    }

    /**
     * Retrieves the subject from claim accessor.
     *
     * @return A {@link Mono} emitting the subject string.
     */
    private Mono<String> getSubjectFromClaimAccessor() {
        return getPrincipalAs(ClaimAccessor.class)
                .map(claimAccessor -> claimAccessor.getClaimAsString(SUBJECT))
                .filter(Objects::nonNull)
                .doOnNext(subjectName -> log.debug("got subject from authentication.principal :: ClaimAccessor, subject is {}", subjectName))
                .onErrorResume(NullPointerException.class, e -> {
                    log.error("NullPointerException in getSubjectFromClaimAccessor", e);
                    return Mono.empty();
                });
    }

    /**
     * Retrieves the subject from the OAuth2 attribute.
     *
     * @return A {@link Mono} emitting the subject string.
     */
    private Mono<String> getSubjectFromOAuth2Attribute() {
        return getAuthentication()
                .map(authentication -> getOAuth2Attribute(authentication, SUBJECT, null))
                .filter(Objects::nonNull)
                .filter(String.class::isInstance)
                .cast(String.class)
                .onErrorResume(NullPointerException.class, e -> {
                    log.error("NullPointerException in getSubjectFromOAuth2Attribute", e);
                    return Mono.empty();
                })
                .doOnNext(subjectName -> log.debug("got subject from plain oath2 attribute, subject is {}", subjectName));
    }

    @Override
    public Mono<String> getIdTokenValue() {
        return getAuthentication()
                .flatMap(this::extractIdTokenValue)
                .doOnError(e -> log.error("Error in getIdTokenValue", e));
    }

    @Override
    public Mono<String> getIdTokenValue(WebFilterExchange exchange) {
        return getIdTokenValue(exchange.getExchange());
    }

    @Override
    public Mono<String> getIdTokenValue(ServerWebExchange exchange) {
        return getAuthentication(exchange)
                .flatMap(this::extractIdTokenValue);
    }

    /**
     * Extracts the ID token value from the authentication.
     *
     * @param authentication The authentication object.
     * @return A {@link Mono} emitting the ID token value.
     */
    protected Mono<String> extractIdTokenValue(Authentication authentication) {
        if (authentication == null) {
            log.error("extractIdTokenValue: Authentication is null");
            return Mono.empty();
        }

        return getPrincipalAs(OidcUser.class, authentication)
                .map(OidcUser::getIdToken)
                .map(OAuth2Token::getTokenValue)
                .doOnNext(token -> log.debug("Got ID Token value from OidcUser: {}", token))
                .onErrorResume(e -> {
                    log.error("Caught error in extractIdTokenValue", e);
                    return Mono.empty();
                })
                .switchIfEmpty(Mono.empty());
    }

    @Override
    public Mono<String> getUserName(){
        return getUserNameFromAuthenticatedPrincipal()
                .switchIfEmpty(this.getUserNameFromJwtAuthenticationToken());
    }

    /**
     * Retrieves the username from the authenticated principal.
     *
     * @return A {@link Mono} emitting the username.
     */
    private Mono<String> getUserNameFromAuthenticatedPrincipal() {
        return getPrincipalAs(AuthenticatedPrincipal.class)
                .map(AuthenticatedPrincipal::getName)
                .doOnNext(userName -> log.debug("got username from authentication.principal :: AuthenticatedPrincipal, name is {}", userName))
                .onErrorResume(NullPointerException.class, e -> {
                    log.error("NullPointerException in getUserNameFromAuthenticatedPrincipal", e);
                    return Mono.empty();
                });
    }

    /**
     * Retrieves the username from a JWT authentication token.
     *
     * @return A {@link Mono} emitting the username.
     */
    private Mono<String> getUserNameFromJwtAuthenticationToken() {
        return getAuthorizationAs(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getName)
                .doOnNext(userName -> log.debug("got username from authentication :: JwtAuthenticationToken, name is {}", userName))
                .onErrorResume(NullPointerException.class, e -> {
                    log.error("NullPointerException in getUserNameFromJwtAuthenticationToken", e);
                    return Mono.empty();
                });
    }

    /**
     * Retrieves the principal as an instance of the specified class.
     *
     * @param clazz The class of the principal.
     * @return A {@link Mono} emitting the principal.
     */
    private <T> Mono<T> getPrincipalAs(Class<T> clazz) {
        return getAuthentication()
                .flatMap(authentication -> this.getPrincipalAs(clazz, authentication))
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("getPrincipalAs returned empty result");
                    return Mono.empty();
                }));
    }

    /**
     * Retrieves the principal as an instance of the specified class from the given authentication.
     *
     * @param clazz The class of the principal.
     * @param authentication The authentication object.
     * @return A {@link Mono} emitting the principal.
     */
    protected <T> Mono<T> getPrincipalAs(Class<T> clazz, Authentication authentication) {
        if (authentication == null) {
            log.error("Authentication is null in getPrincipalAs");
            return Mono.empty();
        }

        return Mono.justOrEmpty(authentication.getPrincipal())
                .filter(clazz::isInstance)
                .cast(clazz)
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("Principal {} is not of the required type {} in getPrincipalAs",
                            authentication.getPrincipal().getClass().getSimpleName(), clazz.getSimpleName());
                    return Mono.empty();
                }))
                .onErrorResume(e -> {
                    log.error("Error in getPrincipalAs", e);
                    return Mono.empty();
                });
    }

    /**
     * Retrieves the principal as an instance of the specified authorization class.
     *
     * @param clazz The class of the authorization.
     * @return A {@link Mono} emitting the authorization.
     */
    private <T> Mono<T> getAuthorizationAs(Class<T> clazz) {
        return getAuthentication()
                .filter(clazz::isInstance)
                .cast(clazz);
    }

    /**
     * Retrieves the principal from the authentication.
     *
     * @param authentication The authentication object.
     * @return An {@link Optional} containing the principal.
     */
    private Optional<Object> getPrincipal(Authentication authentication) {
        if (authentication.getPrincipal() == null) {
            return Optional.empty();
        }
        return Optional.of(authentication.getPrincipal());
    }

    /**
     * Retrieves the principal wrapped in a {@link Mono}.
     *
     * @return A {@link Mono} emitting an {@link Optional} containing the principal.
     */
    protected Mono<Optional<Object>> getPrincipal() {
        return getAuthentication()
                .map(this::getPrincipal)
                .switchIfEmpty(Mono.just(Optional.empty()))
                .onErrorResume(NullPointerException.class, e -> {
                    log.error("NullPointerException in getPrincipal", e);
                    return Mono.empty();
                });
    }

    /**
     * Retrieves the OAuth2 principal from the authentication.
     *
     * @param authentication The authentication object.
     * @return An {@link Optional} containing the {@link OAuth2AuthenticatedPrincipal}.
     */
    private Optional<OAuth2AuthenticatedPrincipal> getOAuth2Principal(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof OAuth2AuthenticatedPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    /**
     * Retrieves an attribute from the OAuth2 principal.
     *
     * @param authentication The authentication object.
     * @param attribute The attribute name.
     * @param fallback The fallback value if the attribute isn't found.
     * @return The attribute value or the fallback value.
     */
    private Object getOAuth2Attribute(Authentication authentication, String attribute, Object fallback) {
        return getOAuth2Principal(authentication)
                .map(principal -> principal.getAttribute(attribute))
                .orElse(fallback);
    }
}