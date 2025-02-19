package de.swm.lhm.geoportal.gateway.authentication.login;

import de.swm.lhm.geoportal.gateway.authentication.LoginLogoutProperties;
import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.just;


class LoginControllerUnitTest extends BaseIntegrationTest {

    private static final MockedStatic<ReactiveSecurityContextHolder> contextHolder = mockStatic(ReactiveSecurityContextHolder.class);

    @Autowired
    private LoginController controller;
    @Autowired
    private LoginLogoutProperties properties;

    @AfterAll
    public static void closeStaticMock() {
        contextHolder.close();
    }

    @Test
    void shouldRespondWithOKIfSecurityContextProvided() {

        // given when
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        contextHolder.when(ReactiveSecurityContextHolder::getContext).thenReturn(just(securityContext));
        ResponseEntity<String> response = controller.checkAuthentication().block();

        // then
        var softly = new SoftAssertions();
        assertThat(response, is(not(nullValue())));
        softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));
        softly.assertAll();
    }

    @Test
    void shouldRespondWithForbiddenIfSecurityContextIsNull() {

        // given when
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        contextHolder.when(ReactiveSecurityContextHolder::getContext).thenReturn(just(securityContext));
        ResponseEntity<String> response = controller.checkAuthentication().block();

        // then
        var softly = new SoftAssertions();
        assertThat(response, is(not(nullValue())));
        softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(403));
        softly.assertAll();
    }

    @Test
    void shouldRespondWithForbiddenIfSecurityContextIsEmpty() {

        // given when
        contextHolder.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.empty());
        ResponseEntity<String> response = controller.checkAuthentication().block();

        // then
        var softly = new SoftAssertions();
        assertThat(response, is(not(nullValue())));
        softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(403));
        softly.assertAll();
    }


    @Test
    void shouldRespondWithForbiddenIfAnonymousAuthenticationTokenProvided() {

        // given when
        contextHolder.when(ReactiveSecurityContextHolder::getContext).thenReturn(just(AnonymousAuthenticationToken.class));
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(mock(AnonymousAuthenticationToken.class));
        contextHolder.when(ReactiveSecurityContextHolder::getContext).thenReturn(just(securityContext));
        ResponseEntity<String> response = controller.checkAuthentication().block();

        // then
        var softly = new SoftAssertions();
        assertThat(response, is(not(nullValue())));
        softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(403));
        softly.assertAll();
    }

    @Test
    void shouldRespondWithHtmlIfLoginSuccessIsCalled() throws IOException {

        byte[] responseBodyAsBytes = webTestClient.get()
                .uri(properties.getLoginSuccess().getEndpoint())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody().returnResult().getResponseBody();

        assertThat(responseBodyAsBytes, is(not(nullValue())));

        String responseBodyAsString = new String(responseBodyAsBytes, StandardCharsets.UTF_8);

        String fileContent = loadFileContent(properties.getLoginSuccess().getPage());

        assertThat(responseBodyAsString, is(fileContent));

    }

}
