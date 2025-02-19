package de.swm.lhm.geoportal.gateway.authorization;

import de.swm.lhm.geoportal.gateway.authentication.basic_auth.BasicAuthServerWebExchangeMatcher;
import de.swm.lhm.geoportal.gateway.authentication.basic_auth.BasicAuthenticationManager;
import de.swm.lhm.geoportal.gateway.filter.webfilter.ArcGisAuthenticationTriggerFilter;
import de.swm.lhm.geoportal.gateway.filter.webfilter.BodyCachingFilter;
import de.swm.lhm.geoportal.gateway.geoservice.GeoServiceProperties;
import de.swm.lhm.geoportal.gateway.authentication.login.LoginPageFilter;
import de.swm.lhm.geoportal.gateway.authentication.logout.LogoutPageFilter;
import de.swm.lhm.geoportal.gateway.shared.GeoPortalGatewayProperties;
import de.swm.lhm.geoportal.gateway.util.messagebody.MessageBodyEncodingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;

import java.util.List;


@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    public static final String ACTUATOR_PATH = "/actuator/**";

    @Value("${geoportal.gateway.login.logincheck-endpoint}")
    private String loginCheckEndpoint;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityWebFilterChain configureBasicAuthHttpSecurity(
            ServerHttpSecurity http,
            BasicAuthenticationManager basicAuthenticationManager,
            List<HasAuthorization> authorizations,
            LogoutPageFilter logoutPageFilter,
            GeoServiceProperties geoServiceProperties,
            MessageBodyEncodingService messageBodyEncodingService,
            GeoPortalGatewayProperties geoPortalGatewayProperties
    ) {
        // process only requests with HTTP Basic Authorization
        // otherwise fall back to next SecurityWebFilterChain in Order
        http
                .securityMatcher(new BasicAuthServerWebExchangeMatcher())
                .httpBasic(httpBasicSpec -> httpBasicSpec.authenticationManager(basicAuthenticationManager));

        return setCommonAttributes(http, authorizations, logoutPageFilter, geoServiceProperties, messageBodyEncodingService, geoPortalGatewayProperties);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    SecurityWebFilterChain configureOAuth2HttpSecurity(
            ServerHttpSecurity http,
            List<HasAuthorization> authorizations,
            LogoutPageFilter logoutPageFilter,
            ServerAuthenticationSuccessHandler serverAuthenticationSuccessHandler,
            GeoServiceProperties geoServiceProperties,
            MessageBodyEncodingService messageBodyEncodingService,
            GeoPortalGatewayProperties geoPortalGatewayProperties
    ) {
        http
                .oauth2Client(Customizer.withDefaults())
                .oauth2Login(oAuth2LoginSpec -> oAuth2LoginSpec.authenticationSuccessHandler(serverAuthenticationSuccessHandler))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .addFilterBefore(new LoginPageFilter(), SecurityWebFiltersOrder.LOGIN_PAGE_GENERATING);

        return setCommonAttributes(http, authorizations, logoutPageFilter, geoServiceProperties, messageBodyEncodingService, geoPortalGatewayProperties);
    }

    SecurityWebFilterChain setCommonAttributes(
            ServerHttpSecurity http,
            List<HasAuthorization> authorizations,
            LogoutPageFilter logoutPageFilter,
            GeoServiceProperties geoServiceProperties,
            MessageBodyEncodingService messageBodyEncodingService,
            GeoPortalGatewayProperties geoPortalGatewayProperties

    ) {

        return http
                .addFilterBefore(logoutPageFilter, SecurityWebFiltersOrder.LOGOUT_PAGE_GENERATING)
                .addFilterBefore(createArcGisAuthenticationTriggerFilter(geoServiceProperties), SecurityWebFiltersOrder.HTTP_BASIC)
                // cache request bodies before any authentication runs, to be able to inspect
                // the bodies for SpringSecurity while preserving them to proxy them to the
                // services behind the gateway
                .addFilterBefore(createBodyCachingFilter(messageBodyEncodingService, geoPortalGatewayProperties), SecurityWebFiltersOrder.HTTP_BASIC)
                .requestCache(Customizer.withDefaults())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .authorizeExchange(
                        authorizeExchangeSpec ->
                                configureAuthorizations(authorizeExchangeSpec, authorizations)
                                        .pathMatchers(loginCheckEndpoint).permitAll()
                                        .pathMatchers(ACTUATOR_PATH).permitAll()
                                        .anyExchange().permitAll())
                .build();

    }

    private ServerHttpSecurity.AuthorizeExchangeSpec configureAuthorizations(
            ServerHttpSecurity.AuthorizeExchangeSpec authorizeExchangeSpec,
            List<HasAuthorization> authorizations
    ) {
        for (HasAuthorization authorization : authorizations) {
            log.debug("registering authorization {}", authorization.info());
            authorizeExchangeSpec.matchers(authorization.getMatcher()).access(authorization.getManager());
        }
        return authorizeExchangeSpec;
    }

    private BodyCachingFilter createBodyCachingFilter(
            MessageBodyEncodingService messageBodyEncodingService,
            GeoPortalGatewayProperties geoPortalGatewayProperties
    ){
        // bodycachingfilter is instantiated here to ensure it is added in the correct order.
        // Creating a spring bean for it would lead to spring automatically adding it without control over
        // the order.
        BodyCachingFilter bodyCachingFilter = new BodyCachingFilter(messageBodyEncodingService);
        if (geoPortalGatewayProperties.getMaxRequestBodySize() != null) {
            bodyCachingFilter.setMaxByteCountForBody(
                    (int) geoPortalGatewayProperties.getMaxRequestBodySize().toBytes()
            );
        } else {
            log.warn("No maximum size for cached request bodies is configured. Continuing without limit.");
        }
        return bodyCachingFilter;
    }

    private ArcGisAuthenticationTriggerFilter createArcGisAuthenticationTriggerFilter(
            GeoServiceProperties geoServiceProperties
    ){
        return new ArcGisAuthenticationTriggerFilter(geoServiceProperties);
    }
}