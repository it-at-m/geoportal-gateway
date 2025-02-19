package de.swm.lhm.geoportal.gateway.m2m;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import de.swm.lhm.geoportal.gateway.authentication.keycloak.KeyCloakService;
import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import de.swm.lhm.geoportal.gateway.m2m.model.M2MCredentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.*;


@TestPropertySource(properties = {
        "geoportal.m2m.eai-url=http://localhost:" + M2MPasswordControllerTest.WIREMOCK_PORT
})
@AutoConfigureWireMock(port = M2MPasswordControllerTest.WIREMOCK_PORT)
@ExtendWith(OutputCaptureExtension.class)
class M2MPasswordControllerTest extends BaseIntegrationTest {

    public static final int WIREMOCK_PORT = 8099;
    private static final String ACCESS_TOKEN = "-<token>-";

    @Autowired
    private M2MProperties m2mProperties;

    @MockBean
    private KeyCloakService keyCloakService;

    private ResponseDefinitionBuilder successfulResponse() {
        return aResponse()
                .withBody("{\"password\": \"charlie\"}")
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withStatus(200);
    }

    private ResponseDefinitionBuilder forbiddenResponse() {
        return aResponse()
                .withBody("""
                {
                  "errorCode": "F900",
                  "errorText": "RH-SSO meldet unautorisierten Zugriff. Ggf. wurde der RH-SSO ohne Token aufgerufen.",
                  "errorDetails": "HTTP operation failed invoking http://svlhmgdii03.muenchen.swm.de:8090/auth/admin/realms/public/users/d575d5ae-4d26-4c1f-ac7a-77a24a3d14f9 with statusCode: 401; {\\"error\\":\\"HTTP 401 Unauthorized\\"}",
                  "executionSteps": {
                    "de.muenchen.rhsso.routes.setUserPassword": false,
                    "de.muenchen.rhsso.routes.validateUserUUID": false,
                    "de.muenchen.rhsso.routes.userExists": false
                  },
                  "responseType": "failure",
                  "resultCode": "1021",
                  "resultText": "Fehler beim Überprüfen, ob der Benutzer existiert. (Schritt: de.muenchen.rhsso.routes.userExists)"
                }""")
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withStatus(403);
    }

    private ResponseDefinitionBuilder forbiddenUnexpectedResponse() {
        return aResponse()
                .withBody("""
                {
                  "test": "Fehler beim Überprüfen, ob der Benutzer existiert. (Schritt: de.muenchen.rhsso.routes.userExists)"
                }""")
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withStatus(403);
    }

    private ResponseDefinitionBuilder internalServerErrorResponse() {
        return aResponse()
                .withBody("""
com.fasterxml.jackson.databind.exc.InvalidDefinitionException: No serializer found for class org.apache.camel.converter.stream.CachedOutputStream$WrappedInputStream and no properties discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)
    at com.fasterxml.jackson.databind.exc.InvalidDefinitionException.from(InvalidDefinitionException.java:77)
    at com.fasterxml.jackson.databind.SerializerProvider.reportBadDefinition(SerializerProvider.java:1277)
    at com.fasterxml.jackson.databind.DatabindContext.reportBadDefinition(DatabindContext.java:400)
    at com.fasterxml.jackson.databind.ser.impl.UnknownSerializer.failForEmpty(UnknownSerializer.java:71)
    at com.fasterxml.jackson.databind.ser.impl.UnknownSerializer.serialize(UnknownSerializer.java:33)
    at com.fasterxml.jackson.databind.ser.DefaultSerializerProvider._serialize(DefaultSerializerProvider.java:480)
    at com.fasterxml.jackson.databind.ser.DefaultSerializerProvider.serializeValue(DefaultSerializerProvider.java:319)
    at com.fasterxml.jackson.databind.ObjectWriter$Prefetch.serialize(ObjectWriter.java:1516)
    at com.fasterxml.jackson.databind.ObjectWriter._writeValueAndClose(ObjectWriter.java:1217)
    at com.fasterxml.jackson.databind.ObjectWriter.writeValue(ObjectWriter.java:1043)
    at org.apache.camel.component.jackson.JacksonDataFormat.marshal(JacksonDataFormat.java:153)
    at org.apache.camel.support.processor.MarshalProcessor.process(MarshalProcessor.java:64)
    at org.apache.camel.impl.engine.DefaultAsyncProcessorAwaitManager.process(DefaultAsyncProcessorAwaitManager.java:83)
    at org.apache.camel.support.AsyncProcessorSupport.process(AsyncProcessorSupport.java:41)
    at org.apache.camel.processor.RestBindingAdvice.marshal(RestBindingAdvice.java:425)
    at org.apache.camel.processor.RestBindingAdvice.after(RestBindingAdvice.java:155)
    at org.apache.camel.processor.RestBindingAdvice.after(RestBindingAdvice.java:54)
    at org.apache.camel.processor.CamelInternalProcessor$AsyncAfterTask.done(CamelInternalProcessor.java:175)
    at org.apache.camel.AsyncCallback.run(AsyncCallback.java:44)
    at org.apache.camel.impl.engine.DefaultReactiveExecutor$Worker.schedule(DefaultReactiveExecutor.java:148)
    at org.apache.camel.impl.engine.DefaultReactiveExecutor.scheduleMain(DefaultReactiveExecutor.java:60)
    at org.apache.camel.processor.Pipeline.process(Pipeline.java:147)
    at org.apache.camel.processor.CamelInternalProcessor.process(CamelInternalProcessor.java:288)
    at org.apache.camel.impl.engine.DefaultAsyncProcessorAwaitManager.process(DefaultAsyncProcessorAwaitManager.java:83)
    at org.apache.camel.support.AsyncProcessorSupport.process(AsyncProcessorSupport.java:41)
    at org.apache.camel.http.common.CamelServlet.doService(CamelServlet.java:219)
    at org.apache.camel.http.common.CamelServlet.service(CamelServlet.java:81)
    at javax.servlet.http.HttpServlet.service(HttpServlet.java:733)
    at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:231)
    at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
    at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:53)
    at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
    at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
    at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100)
    at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
    at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
    at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
    at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93)
    at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
    at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
    at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
    at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201)
    at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:119)
    at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
    at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
    at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:202)
    at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:97)
    at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:542)
    at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:143)
    at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:92)
    at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:78)
    at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:343)
    at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:374)
    at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:65)
    at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:868)
    at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1590)
    at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49)
    at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
    at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
    at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)
    at java.base/java.lang.Thread.run(Thread.java:1583)
                        """)
                .withStatus(500);
    }

    private MappingBuilder baseEaiMock(){
        when(keyCloakService.getClientCredentialsGrantedAccessToken())
                .thenReturn(Mono.just(ACCESS_TOKEN));
        return put(urlPathMatching("/api/user/.*/password"));
    }

    private void mockEaiServerWithSuccessResponse() {
        stubFor(baseEaiMock().willReturn(successfulResponse()));
    }

    private void mockEaiServerWithForbiddenResponse() {
        stubFor(baseEaiMock().willReturn(forbiddenResponse())
        );
    }

    private void mockEaiServerWithForbiddenUnexpectedResponse() {
        stubFor(baseEaiMock().willReturn(forbiddenUnexpectedResponse())
        );
    }

    private void mockEaiServerWithInternalServerErrorResponse() {
        stubFor(baseEaiMock().willReturn(internalServerErrorResponse()));
    }

    @Test
    void generatePasswordWithoutLoginShouldFail() {
        expectRedirectToKeyCloak(
                webTestClient
                        .put()
                        .uri(m2mProperties.getPasswordEndpoint())
                        .exchange()
        );
    }

    @Test
    void generatePasswordSucceedsWithOidcLogin() throws IOException {

        mockEaiServerWithSuccessResponse();

        byte[] bytesResult = webTestClient
                .mutateWith(
                        mockOidcLogin()
                )
                .put()
                .uri(m2mProperties.getPasswordEndpoint())
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();

        M2MCredentials credentials = new ObjectMapper().readValue(bytesResult, M2MCredentials.class);

        assertThat(credentials.getPassword(), is("charlie"));

        verify(
                putRequestedFor(urlPathMatching("/api/user/.*/password"))
                        .withHeader("Authorization", equalTo("Bearer %s".formatted(ACCESS_TOKEN)))
        );

    }

    @Test
    void generatePasswordReturnsForbiddenWithOidcLogin() {

        mockEaiServerWithForbiddenResponse();

        webTestClient
                .mutateWith(
                        mockOidcLogin()
                )
                .put()
                .uri(m2mProperties.getPasswordEndpoint())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void generatePasswordReturnsForbiddenUnexpectedWithOidcLogin(CapturedOutput output) {

        mockEaiServerWithForbiddenUnexpectedResponse();

        webTestClient
                .mutateWith(
                        mockOidcLogin()
                )
                .put()
                .uri(m2mProperties.getPasswordEndpoint())
                .exchange()
                .expectStatus().isForbidden();

        assertThat(output.getAll(), containsString("\"test\": \"Fehler beim Überprüfen, ob der Benutzer existiert. (Schritt: de.muenchen.rhsso.routes.userExists)\""));
    }

    @Test
    void generatePasswordReturnsInternalServerErrorWithOidcLogin(CapturedOutput output) {

        mockEaiServerWithInternalServerErrorResponse();

        webTestClient
                .mutateWith(
                        mockOidcLogin()
                )
                .put()
                .uri(m2mProperties.getPasswordEndpoint())
                .exchange()
                .expectStatus().is5xxServerError();

        assertThat(output.getAll(), containsString("com.fasterxml.jackson.databind.exc.InvalidDefinitionException"));

    }

    @Test
    void generatePasswordWithOAuthLoginShouldFail() {
        webTestClient
                .mutateWith(
                        mockOAuth2Login()
                )
                .put()
                .uri(m2mProperties.getPasswordEndpoint())
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void generatePasswordWithOpaqueLoginShouldFail() {
        webTestClient
                .mutateWith(
                        mockOpaqueToken()
                )
                .put()
                .uri(m2mProperties.getPasswordEndpoint())
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void generatePasswordWithJwtLogin() throws IOException {

        mockEaiServerWithSuccessResponse();

        byte[] bytesResult = webTestClient
                .mutateWith(
                        mockJwt()
                )
                .put()
                .uri(m2mProperties.getPasswordEndpoint())
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();

        M2MCredentials credentials = new ObjectMapper().readValue(bytesResult, M2MCredentials.class);

        assertThat(credentials.getPassword(), is("charlie"));

        verify(
                putRequestedFor(urlPathMatching("/api/user/.*/password"))
                        .withHeader("Authorization", equalTo("Bearer %s".formatted(ACCESS_TOKEN)))
        );

    }

}