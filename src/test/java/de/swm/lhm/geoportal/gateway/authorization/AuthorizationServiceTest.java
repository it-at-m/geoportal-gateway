package de.swm.lhm.geoportal.gateway.authorization;

import de.swm.lhm.geoportal.gateway.authorization.model.StorkQaaLevel;
import de.swm.lhm.geoportal.gateway.authorization.repository.AuthorizationInfoRepository;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import groovy.util.logging.Slf4j;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static de.swm.lhm.geoportal.gateway.authentication.TokenConstants.AUTHORITIES;
import static de.swm.lhm.geoportal.gateway.authentication.TokenConstants.AUTH_LEVEL;
import static de.swm.lhm.geoportal.gateway.authorization.model.StorkQaaLevel.STORK_QAA_LEVEL_1;
import static de.swm.lhm.geoportal.gateway.authorization.model.StorkQaaLevel.STORK_QAA_LEVEL_3;
import static de.swm.lhm.geoportal.gateway.authorization.model.StorkQaaLevel.STORK_QAA_LEVEL_4;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;


@Slf4j
class AuthorizationServiceTest {

    final String lexicon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ12345674890";
    final Random rand = new SecureRandom();
    final Set<String> identifiers = new HashSet<>();
    final StorkQaaLevel defaultAuthLevel = STORK_QAA_LEVEL_4;
    final List<String> defaultProducts = List.of("Product_1, Product_2, Product_3, Product_4, Product_5, Product_6");

    @Test
    void shouldReturnNoGrantedProductsWhenPrincipalIsNotOAuth2Authenticated() {
        // given
        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));
        Authentication authentication = mock(Authentication.class);
        OAuth2AuthenticatedPrincipal principal = mock(OAuth2AuthenticatedPrincipal.class);
        when(principal.getAttribute(AUTHORITIES)).thenReturn(1.0);
        when(authentication.getPrincipal()).thenReturn(principal);

        // when
        Set<String> grantedProducts = authorizationService.getGrantedProducts(authentication);

        // then
        Assertions.assertThat(grantedProducts).isEmpty();
    }

    @Test
    void shouldReturnNoGrantedProductsWhenAuthoritiesIsNull() {
        // given
        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));
        Authentication authentication = mock(Authentication.class);
        OAuth2AuthenticatedPrincipal principal = mock(OAuth2AuthenticatedPrincipal.class);
        when(principal.getAttribute(AUTHORITIES)).thenReturn(null);
        when(authentication.getPrincipal()).thenReturn(principal);

        // when
        Set<String> grantedProducts = authorizationService.getGrantedProducts(authentication);

        // then
        Assertions.assertThat(grantedProducts).isEmpty();
    }

    @Test
    void shouldReturnNoGrantedProductsWhenAuthoritiesAreEmpty() {
        // given
        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));
        Authentication authentication = mock(Authentication.class);
        OAuth2AuthenticatedPrincipal principal = mock(OAuth2AuthenticatedPrincipal.class);
        when(principal.getAttribute(AUTHORITIES)).thenReturn(List.of());
        when(authentication.getPrincipal()).thenReturn(principal);

        // when
        Set<String> grantedProducts = authorizationService.getGrantedProducts(authentication);

        // then
        Assertions.assertThat(grantedProducts).isEmpty();
    }

    @Test
    void shouldNotGrantAuthLevelHighIfNoPrincipal() {
        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(null);

        Assertions.assertThat(authorizationService.getGrantedAuthLevelHigh(authentication)).isFalse();
    }

    @Test
    void shouldNotGrantAuthLevelHighIfQAALevelIs1() {
        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));
        Authentication authentication = mock(Authentication.class);
        OAuth2AuthenticatedPrincipal principal = mock(OAuth2AuthenticatedPrincipal.class);
        when(principal.getAttribute(AUTH_LEVEL)).thenReturn(STORK_QAA_LEVEL_1);
        when(authentication.getPrincipal()).thenReturn(principal);

        Assertions.assertThat(authorizationService.getGrantedAuthLevelHigh(authentication)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = StorkQaaLevel.class, names = {"STORK_QAA_LEVEL_3", "STORK_QAA_LEVEL_4"})
    void shouldGrantAuthLevelHighForQAALevel3or4(StorkQaaLevel storkLevel) {
        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));
        Authentication authentication = mock(Authentication.class);
        OAuth2AuthenticatedPrincipal principal = mock(OAuth2AuthenticatedPrincipal.class);
        when(principal.getAttribute(AUTH_LEVEL)).thenReturn(storkLevel);
        when(authentication.getPrincipal()).thenReturn(principal);

        Assertions.assertThat(authorizationService.getGrantedAuthLevelHigh(authentication)).isTrue();
    }

    @Test
    void shouldReturnUserNameFromOidc() {
        String username = createUserName();
        Authentication auth = createOidcAuthenticationWithUserName(username);
        AuthorizationService authorizationService = mockAuthService();
        when(authorizationService.getAuthentication()).thenReturn(Mono.just(auth));
        when(authorizationService.getUserName()).thenCallRealMethod();

        StepVerifier.create(authorizationService.getUserName())
                .expectNext(username)
                .expectComplete()
                .verify();
    }

    @Test
    void shouldReturnUserNameFromJwt() {
        String username = createUserName();
        Authentication auth = createJwtAuthenticationWithUserName(username);
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        when(authorizationService.getAuthentication()).thenReturn(Mono.just(auth));
        when(authorizationService.getUserName()).thenCallRealMethod();

        StepVerifier.create(authorizationService.getUserName())
                .expectNext(username)
                .expectComplete()
                .verify();
    }

    @Test
    void shouldHandleNullOidcUserNameGracefully() {
        Authentication auth = createOidcAuthenticationWithUserName(null);
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        when(authorizationService.getAuthentication()).thenReturn(Mono.just(auth));
        when(authorizationService.getUserName()).thenCallRealMethod();

        StepVerifier.create(authorizationService.getUserName())
                .expectNextCount(0)
                .expectComplete()
                .verify();
    }

    @Test
    void shouldHandleNullJwtUserNameGracefully() {
        Authentication auth = createJwtAuthenticationWithUserName(null);
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        when(authorizationService.getAuthentication()).thenReturn(Mono.just(auth));
        when(authorizationService.getUserName()).thenCallRealMethod();

        StepVerifier.create(authorizationService.getUserName())
                .expectNextCount(0)
                .expectComplete()
                .verify();
    }

    @Test
    void shouldReturnSubjectFromOidc() {
        String subject = createSubject();
        Authentication auth = createOidcAuthenticationWithSubject(subject);
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        when(authorizationService.getAuthentication()).thenReturn(Mono.just(auth));
        when(authorizationService.getSubject()).thenCallRealMethod();

        StepVerifier.create(authorizationService.getSubject())
                .expectNext(subject)
                .expectComplete()
                .verify();
    }

    @Test
    void shouldReturnSubjectFromJwt() {
        String subject = createSubject();
        Authentication auth = createJwtAuthenticationWithSubject(subject);
        AuthorizationService authorizationService = mockAuthService();

        doReturn(Mono.just(auth)).when(authorizationService).getAuthentication();

        StepVerifier.create(authorizationService.getSubject())
                .expectNext(subject)
                .expectComplete()
                .verify();
    }

    @Test
    void shouldHandleNullOidcSubjectGracefully() {
        Authentication authOidc = createOidcAuthenticationWithSubject(null);
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        when(authorizationService.getAuthentication()).thenReturn(Mono.just(authOidc));
        when(authorizationService.getSubject()).thenCallRealMethod();

        StepVerifier.create(authorizationService.getSubject())
                .expectNextCount(0)
                .expectComplete()
                .verify();
    }

    @Test
    void shouldHandleNullJwtSubjectGracefully() {
        Authentication authOidc = createJwtAuthenticationWithSubject(null);
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        when(authorizationService.getAuthentication()).thenReturn(Mono.just(authOidc));
        when(authorizationService.getSubject()).thenCallRealMethod();

        StepVerifier.create(authorizationService.getSubject())
                .expectNextCount(0)
                .expectComplete()
                .verify();
    }

    @Test
    void shouldExtractTokenValueFromOidc() {
        String token = createToken();
        Authentication authentication = createOidcAuthenticationWithToken(token);
        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));
        Mono<String> tokenValueMono = authorizationService.extractIdTokenValue(authentication);

        StepVerifier.create(tokenValueMono)
                .expectNext(token)
                .verifyComplete();
    }

    @Test
    void shouldGetIdTokenValueFromOidcUser() {

        String token = createToken();
        Authentication authentication = createOidcAuthenticationWithToken(token);
        
        AuthorizationService authorizationService = mockAuthService();

        when(authorizationService.getAuthentication()).thenReturn(Mono.just(authentication));
        when(authorizationService.extractIdTokenValue(authentication)).thenCallRealMethod();
        when(authorizationService.getIdTokenValue()).thenCallRealMethod();

        StepVerifier.create(authorizationService.getIdTokenValue())
            .expectNext(token)
            .verifyComplete();
    }

    @Test
    void shouldGetIdTokenValueFromWebFilterExchange() {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        WebFilterExchange webFilterExchange = mock(WebFilterExchange.class);
        WebSession webSession = mock(WebSession.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        String token = createToken();
        Authentication authentication = createOidcAuthenticationWithToken(token);
        when(webFilterExchange.getExchange()).thenReturn(exchange);
        when(exchange.getSession()).thenReturn(Mono.just(webSession));
        when(webSession.getAttribute(SPRING_SECURITY_CONTEXT_KEY)).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        AuthorizationService authorizationService = mockAuthService();
        doReturn(Mono.just(authentication)).when(authorizationService).getAuthentication(exchange);

        Mono<String> tokenValueMono = authorizationService.getIdTokenValue(webFilterExchange);

        StepVerifier.create(tokenValueMono)
            .expectNext(token)
            .verifyComplete();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnCorrectGrantedAuthLevelHigh(boolean expectedAuthLevel) {
        Authentication authentication = mock(Authentication.class);
        OAuth2AuthenticatedPrincipal principal = mock(OAuth2AuthenticatedPrincipal.class);
        when(principal.getAttribute(AUTH_LEVEL)).thenReturn(
                expectedAuthLevel ? STORK_QAA_LEVEL_3 : STORK_QAA_LEVEL_1
        );
        when(authentication.getPrincipal()).thenReturn(principal);

        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));
        boolean isAuthLevelHigh = authorizationService.getGrantedAuthLevelHigh(authentication);

        Assertions.assertThat(isAuthLevelHigh).isEqualTo(expectedAuthLevel);
    }

    @Test
    void shouldGetAuthenticationSuccessfully() {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        WebSession webSession = mock(WebSession.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);

        when(exchange.getSession()).thenReturn(Mono.just(webSession));
        when(webSession.getAttribute(SPRING_SECURITY_CONTEXT_KEY)).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));

        Mono<Authentication> authenticationMono = authorizationService.getAuthentication(exchange);

        StepVerifier.create(authenticationMono)
                .expectNext(authentication)
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyAuthenticationWhenNoSession() {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(exchange.getSession()).thenReturn(Mono.empty());

        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));

        Mono<Authentication> authenticationMono = authorizationService.getAuthentication(exchange);

        StepVerifier.create(authenticationMono)
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyAuthenticationWhenNoSecurityContext() {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        WebSession webSession = mock(WebSession.class);

        when(exchange.getSession()).thenReturn(Mono.just(webSession));
        when(webSession.getAttribute(SPRING_SECURITY_CONTEXT_KEY)).thenReturn(null);

        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));

        Mono<Authentication> authenticationMono = authorizationService.getAuthentication(exchange);

        StepVerifier.create(authenticationMono)
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyAuthenticationOnException() {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(exchange.getSession()).thenThrow(new NullPointerException());

        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));

        Mono<Authentication> authenticationMono = authorizationService.getAuthentication(exchange);

        StepVerifier.create(authenticationMono)
                .verifyComplete();
    }

    @Test
    void shouldGetIdTokenValueFromServerWebExchangeSuccessfully() {
        AuthorizationService authorizationService = mockAuthService();
        
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        Authentication authentication = mock(Authentication.class);
        String tokenValue = "test-token";

        when(authorizationService.getAuthentication(exchange)).thenReturn(Mono.just(authentication));
        when(authorizationService.extractIdTokenValue(authentication)).thenReturn(Mono.just(tokenValue));

        Mono<String> result = authorizationService.getIdTokenValue(exchange);

        StepVerifier.create(result)
                .expectNext(tokenValue)
                .verifyComplete();
    }

    @Test
    void shouldGetIdTokenValueFromWebFilterExchangeSuccessfully() {
        AuthorizationService authorizationService = mockAuthService();
        
                ServerWebExchange exchange = mock(ServerWebExchange.class);
        WebFilterExchange webFilterExchange = mock(WebFilterExchange.class);
        when(webFilterExchange.getExchange()).thenReturn(exchange);
        Authentication authentication = mock(Authentication.class);
        String tokenValue = "test-token";

        when(authorizationService.getAuthentication(exchange)).thenReturn(Mono.just(authentication));
        when(authorizationService.extractIdTokenValue(authentication)).thenReturn(Mono.just(tokenValue));

        Mono<String> result = authorizationService.getIdTokenValue(webFilterExchange);

        StepVerifier.create(result)
                .expectNext(tokenValue)
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyTokenValueWhenNoAuthentication() {
        AuthorizationService authorizationService = mockAuthService();
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(authorizationService.getAuthentication()).thenReturn(Mono.empty());
        when(authorizationService.getAuthentication(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        Mono<String> result = authorizationService.getIdTokenValue(exchange);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldNotExtractIdTokenValueFromOAuth2Token() {

        AuthorizationService authorizationService = mockAuthService();
        Authentication authentication = mock(Authentication.class);
        OAuth2Token token = mock(OAuth2AccessToken.class);

        when(authentication.getPrincipal()).thenReturn(token);


        Mono<String> result = authorizationService.extractIdTokenValue(authentication);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldExtractTokenValueFromOidcUser() {
        AuthorizationService authorizationService = mockAuthService();
        Authentication authentication = mock(Authentication.class);
        OidcUser oidcUser = mock(OidcUser.class);
        OidcIdToken token = mock(OidcIdToken.class);
        String tokenValue = "test-token";

        when(authentication.getPrincipal()).thenReturn(oidcUser);
        when(oidcUser.getIdToken()).thenReturn(token);
        when(token.getTokenValue()).thenReturn(tokenValue);

        Mono<String> result = authorizationService.extractIdTokenValue(authentication);

        StepVerifier.create(result)
                .expectNext(tokenValue)
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyTokenValueWhenNoPrincipal() {
        AuthorizationService authorizationService = mockAuthService();
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(null);

        Mono<String> result = authorizationService.extractIdTokenValue(authentication);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyForInvalidPrincipalType() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new Object()); // Invalid type for testing

        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));
        Mono<OidcUser> principalMono = authorizationService.getPrincipalAs(OidcUser.class, authentication);

        StepVerifier.create(principalMono)
                    .verifyComplete();
    }

    @Test
    void shouldReturnGrantedAuthLevelHighMonoVersion() {
        Authentication authentication = mock(Authentication.class);
        OAuth2AuthenticatedPrincipal principal = mock(OAuth2AuthenticatedPrincipal.class);
        when(principal.getAttribute(AUTH_LEVEL)).thenReturn(StorkQaaLevel.STORK_QAA_LEVEL_3.getValue());
        when(authentication.getPrincipal()).thenReturn(principal);

        AuthorizationService authorizationService = mockAuthService();
        doReturn(Mono.just(authentication)).when(authorizationService).getAuthentication();

        Mono<Boolean> result = authorizationService.getGrantedAuthLevelHigh();

        StepVerifier.create(result)
                    .expectNext(true)
                    .verifyComplete();
    }

    @ParameterizedTest
    @EnumSource(value = StorkQaaLevel.class)
    void shouldProperlyHandleVariousAuthLevels(StorkQaaLevel storkLevel) {
        Authentication authentication = mock(Authentication.class);
        OAuth2AuthenticatedPrincipal principal = mock(OAuth2AuthenticatedPrincipal.class);
        when(principal.getAttribute(AUTH_LEVEL)).thenReturn(storkLevel);
        when(authentication.getPrincipal()).thenReturn(principal);

        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));
        boolean isAuthLevelHigh = authorizationService.getGrantedAuthLevelHigh(authentication);

        if (storkLevel.equals(STORK_QAA_LEVEL_3) || storkLevel.equals(STORK_QAA_LEVEL_4)) {
            Assertions.assertThat(isAuthLevelHigh).isTrue();
        } else {
            Assertions.assertThat(isAuthLevelHigh).isFalse();
        }
    }

    @Test
    void shouldHandleNoIdTokenForJwtAuthentication() {
        // Überprüfen, dass bei JWT keine ID-Token extrahiert werden, da diese nicht existieren
        Authentication authentication = createJwtAuthentication();
        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));

        Mono<String> idTokenValueMono = authorizationService.extractIdTokenValue(authentication);

        StepVerifier.create(idTokenValueMono)
                .expectNextCount(0) // Hier erwarten wir keinen ID-Token
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyForNullAuthentication() {
        AuthorizationService authorizationService = mockAuthService();
        when(authorizationService.getAuthentication()).thenReturn(Mono.empty());

        Mono<String> result = authorizationService.getIdTokenValue();

        StepVerifier.create(result)
                    .verifyComplete();
    }

    @Test
    void shouldReturnEmptyForInvalidTokenType() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new Object()); // Dummy object for invalid token type

        AuthorizationService authorizationService = mockAuthService();
        Mono<String> result = authorizationService.extractIdTokenValue(authentication);

        StepVerifier.create(result)
                    .verifyComplete();
    }

    @Test
    void shouldReturnEmptyForNullPrincipalInJwt() {
        final String userName = null;
        Authentication auth = createJwtAuthenticationWithUserName(userName);
        AuthorizationService authorizationService = mockAuthService();
        when(authorizationService.getAuthentication()).thenReturn(Mono.just(auth));
        when(authorizationService.getUserName()).thenCallRealMethod();

        StepVerifier.create(authorizationService.getUserName())
                    .expectNextCount(0)
                    .verifyComplete();
    }

    @Test
    void shouldReturnEmptyForNullPrincipalInOidc() {
        final String userName = null;
        Authentication auth = createOidcAuthenticationWithUserName(userName);
        AuthorizationService authorizationService = mockAuthService();
        when(authorizationService.getAuthentication()).thenReturn(Mono.just(auth));
        when(authorizationService.getUserName()).thenCallRealMethod();

        StepVerifier.create(authorizationService.getUserName())
                    .expectNextCount(0)
                    .verifyComplete();
    }

    /* getProducts */

    @Test
    void shouldReturnNoGrantedProductsWhenPrincipalIsNull() {
        // given
        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(null);

        // when
        Set<String> grantedProducts = authorizationService.getGrantedProducts(authentication);

        // then
        assertThat(grantedProducts, is(empty()));
    }

    @Test
    void shouldReturnGrantedProductsForBasicAuth() {
        // given
        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));
        Authentication authentication = createUserPasswordAuthentication();
        // when
        Set<String> grantedProducts = authorizationService.getGrantedProducts(authentication);

        // then
        assertThat(grantedProducts, containsInAnyOrder(defaultProducts.toArray()));

    }

    @Test
    void shouldReturnNoGrantedProductsWhenAuthoritiesListIsEmpty() {
        // given
        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));
        Authentication authentication = mock(Authentication.class);
        OAuth2AuthenticatedPrincipal principal = mock(OAuth2AuthenticatedPrincipal.class);
        when(principal.getAttribute(AUTHORITIES)).thenReturn(Collections.emptyList());
        when(authentication.getPrincipal()).thenReturn(principal);

        // when
        Set<String> grantedProducts = authorizationService.getGrantedProducts(authentication);

        // then
        Assertions.assertThat(grantedProducts).isEmpty();
    }

    @Test
    void shouldReturnNoGrantedProductsWhenAuthoritiesListContainsInvalidEntries() {
        // given
        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));
        Authentication authentication = mock(Authentication.class);
        OAuth2AuthenticatedPrincipal principal = mock(OAuth2AuthenticatedPrincipal.class);
        when(principal.getAttribute(AUTHORITIES)).thenReturn(List.of(1.0, true, new SimpleGrantedAuthority("test"))); // Ungültige Einträge
        when(authentication.getPrincipal()).thenReturn(principal);

        // when
        Set<String> grantedProducts = authorizationService.getGrantedProducts(authentication);

        // then
        Assertions.assertThat(grantedProducts).isEmpty();
    }

    @Test
    void shouldReturnGrantedProductsForOAuth2AuthenticationWithStrings() {
        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));
        Authentication authentication = mock(Authentication.class);
        OAuth2AuthenticatedPrincipal principal = mock(OAuth2AuthenticatedPrincipal.class);
        when(principal.getAttributes()).thenReturn(Map.of(AUTHORITIES, defaultProducts));
        when(authentication.getPrincipal()).thenReturn(principal);

        Set<String> grantedProducts = authorizationService.getGrantedProducts(authentication);

        assertThat(grantedProducts, containsInAnyOrder(defaultProducts.toArray()));
    }

    @Test
    void shouldReturnGrantedProductsForOAuth2AuthenticationWithGrantedAuthorities() {
        AuthorizationService authorizationService = new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class));
        Authentication authentication = mock(Authentication.class);
        OAuth2AuthenticatedPrincipal principal = mock(OAuth2AuthenticatedPrincipal.class);
        when(principal.getAttributes()).thenReturn(Map.of(AUTHORITIES, createProductAuthorities()));
        when(authentication.getPrincipal()).thenReturn(principal);

        Set<String> grantedProducts = authorizationService.getGrantedProducts(authentication);

        assertThat(grantedProducts, containsInAnyOrder(defaultProducts.toArray()));
    }

    /* utility methods */

    private AuthorizationService mockAuthService() {
        return Mockito.spy(new AuthorizationService(mock(AuthorizationInfoRepository.class), mock(GatewayService.class)));
    }
    
    private String createSubject() {
        return UUID.randomUUID().toString();
    }

    private String createToken() {
        byte[] randomBytes = new byte[24];
        rand.nextBytes(randomBytes);
        return Base64.getUrlEncoder().encodeToString(randomBytes);
    }

    private String createUserName() {
        StringBuilder builder = new StringBuilder();
        while(builder.toString().isEmpty()) {
            int length = rand.nextInt(5)+5;
            for(int i = 0; i < length; i++) {
                builder.append(lexicon.charAt(rand.nextInt(lexicon.length())));
            }
            if(identifiers.contains(builder.toString())) {
                builder = new StringBuilder();
            }
        }
        return builder.toString();
    }

    @SneakyThrows
    private Map<String, Object> createAllAttributes(Instant now, Instant exp, String subject, String userName, StorkQaaLevel authLevel, List<String> products) {

        HashMap<String, Object> result = new HashMap<>();

        result.put("acr", "1");
        result.put("azp", "geoPortal-K");
        result.put("aud", List.of("GeoPortal-K"));
        result.put("iat", now);
        result.put("typ", "ID");
        result.put("iss", URI.create("http://localhost:8090/auth/realms/public").toURL());
        result.put("exp", exp);
        result.put("jti", "9fc08ba9-1582-436b-81e4-3645ecdb2d26");

        result.put("realm_access", Map.of("roles",  List.of("offline_access", "uma_authorization")));
        result.put("auth_time", now);

        result.put("session_state", "deece14b-9037-4bda-acb5-cf62bdc7da6a");
        result.put("nonce", "-<nonce>-");

        result.putAll(createUserInfoAttributes(subject, userName, authLevel, products));

        return result;

    }

    private Map<String, Object> createUserInfoAttributes(String subject, String userName, StorkQaaLevel authLevel, List<String> products) {

        HashMap<String, Object> result = new HashMap<>();

        result.put("sub", subject);

        result.put("user_name", userName);
        result.put("preferred_username", userName);
        result.put("given_name", "gsm");
        result.put("family_name", "user4");
        result.put("name", "gsm_user4");

        result.put("email", "gsm_user4@gsm.muc");
        result.put("email_verified", false);
        result.put("address", Map.of());

        result.put("postkorbMailAddress", " null@bsp-postkorb-pre.akdb.doi-de.net");

        result.put("user_roles", List.of("ROLE_ITM", "ROLE_ITM_nonComposite", "ROLE_GSM", "ROLE_GSM_nonComposite", "ROLE_THW", "ROLE_THW_nonComposite"));
        result.put("authlevel", authLevel);
        result.put("authorities", products);

        return result;

    }

    private OidcIdToken createIdToken(Instant now, Instant exp, String subject, String userName, String token, StorkQaaLevel authLevel, List<String> products) {
        return new OidcIdToken(token, now, exp, createAllAttributes(now, exp, subject, userName, authLevel, products));
    }

    private OidcUserInfo createUserInfo(String subject, String userName,StorkQaaLevel authLevel, List<String> products) {
        return new OidcUserInfo(createUserInfoAttributes(subject, userName, authLevel, products));
    }


    private Collection<? extends GrantedAuthority> createOidcAuthorities(OidcIdToken idToken, OidcUserInfo userInfo) {
        HashSet<GrantedAuthority> result = new HashSet<>();
        result.add(new OidcUserAuthority("OIDC_USER", idToken, userInfo));
        result.addAll(createSimpleAuthorities());
        return result;
    }

    private Collection<? extends GrantedAuthority> createSimpleAuthorities() {
        return Set.of(
                new SimpleGrantedAuthority("SCOPE_email"),
                new SimpleGrantedAuthority("SCOPE_openid"),
                new SimpleGrantedAuthority("SCOPE_profile")
        );
    }

    private Authentication createOidcAuthenticationWithSubject(String subject) {
        return createOidcAuthentication(subject, createUserName(), createToken());
    }

    private Authentication createOidcAuthenticationWithUserName(String userName) {
        return createOidcAuthentication(createSubject(), userName, createToken());
    }

    private Authentication createOidcAuthenticationWithToken(String token) {
        return createOidcAuthentication(createSubject(), createUserName(), token);
    }

    private Authentication createOidcAuthentication(String subject, String userName, String token) {
        return createOidcAuthentication(subject, userName, token, defaultAuthLevel, defaultProducts);
    }

    private Authentication createOidcAuthentication(String subject, String userName, String token, StorkQaaLevel authLevel, List<String> products) {
        Instant now = Instant.now();
        Instant exp = now.plus(3, HOURS);

        OidcIdToken idToken = createIdToken(now, exp, subject, userName, token, authLevel, products);
        OidcUserInfo userInfo = createUserInfo(subject, userName, authLevel, products);
        Collection<? extends GrantedAuthority> authorities = createOidcAuthorities(idToken , userInfo);
        OAuth2User principal  = new DefaultOidcUser(authorities, idToken, userInfo, "preferred_username");

        String authorizedClientRegistrationId = "keycloak";

        return new OAuth2AuthenticationToken(principal, authorities, authorizedClientRegistrationId);
    }

    private Authentication createJwtAuthenticationWithSubject(String subject) {
        return createJwtAuthentication(subject, createUserName(), createToken());
    }

    private Authentication createJwtAuthenticationWithUserName(String userName) {
        return createJwtAuthentication(createSubject(), userName, createToken());
    }

    private Authentication createJwtAuthentication() {
        return createJwtAuthentication(createSubject(), createUserName(), createToken());
    }

    private Authentication createJwtAuthentication(String subject, String userName, String token) {
        Instant now = Instant.now();
        Instant exp = now.plus(3, HOURS);

        Jwt jwt = new Jwt(token, now, exp, Map.of("alg", JwsAlgorithms.RS256), createAllAttributes(now, exp, subject, userName, defaultAuthLevel, defaultProducts));

        return new JwtAuthenticationToken(jwt, createSimpleAuthorities(), userName);
    }

    private Collection<? extends GrantedAuthority> createProductAuthorities() {
        return defaultProducts.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    private Authentication createUserPasswordAuthentication() {
            return new UsernamePasswordAuthenticationToken(createUserName() , null, createProductAuthorities());
    }

}