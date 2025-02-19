package de.swm.lhm.geoportal.gateway.geoservice;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import de.swm.lhm.geoportal.gateway.authorization.model.AccessLevel;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;


@AutoConfigureWireMock(port = AbstractGeoServiceTest.WIREMOCK_PORT)
@TestPropertySource(properties = {
        "geoportal.geoserver.url=lb://geoserver",
        "geoportal.gateway.load-balancer.services.geoserver.urls=http://localhost:" + AbstractGeoServiceTest.WIREMOCK_PORT,
        "geoportal.geoserver.max-xml-parsing-duration-ms=400"
})
public abstract class AbstractGeoServiceTest extends BaseIntegrationTest {

    protected final static String PUBLIC_LAYER = "myws:publiclayer";
    protected final static String PROTECTED_LAYER = "myws:protectedlayer";
    protected final static String PROTECTED_LAYER2 = "myws:protectedlayer2";
    protected final static String PROTECTED_AUTH_LEVEL_HIGH_LAYER = "myws:protectedhighlayer";
    protected final static String RESTRICTED_LAYER = "myws:restrictedlayer";

    public final static int WIREMOCK_PORT = 8086;
    protected final static String GEOSERVER_RESPONSE_BODY = "beep";
    protected final static String GEOSERVER_SERVER_HEADER = "geoserver";


    @BeforeEach
    void createDbState() {
        // somewhat hacky db initialization
        runSql(
                """
                        create table geoservice_product_roles_view (
                            resource_id text,
                            stage character varying(255),
                            auth_level_high boolean,
                            access_level character varying(11),
                            geoservice_id integer,
                            role_name character varying(256)
                        )
                 """);

        runSql(
                        "insert into geoservice_product_roles_view " +
                                "(resource_id, stage, auth_level_high, access_level, geoservice_id, role_name) " +
                                " values " +
                                "('" + PUBLIC_LAYER + "', '" + Stage.CONFIGURATION + "', false, '" + AccessLevel.PUBLIC + "', 1, '" + PUBLIC_PRODUCT + "')," +
                                "('" + PROTECTED_LAYER + "', '" + Stage.CONFIGURATION + "', false, '" + AccessLevel.PROTECTED + "', 2, '" + PROTECTED_PRODUCT + "')," +
                                "('" + PROTECTED_LAYER2 + "', '" + Stage.CONFIGURATION + "', false, '" + AccessLevel.PROTECTED + "', 5, '" + PROTECTED_PRODUCT2 + "')," +
                                "('" + PROTECTED_AUTH_LEVEL_HIGH_LAYER + "', '" + Stage.CONFIGURATION + "', true, '" + AccessLevel.PROTECTED + "', 3, '" + PROTECTED_AUTH_LEVEL_HIGH_PRODUCT + "')," +
                                "('" + RESTRICTED_LAYER + "', '" + Stage.CONFIGURATION + "', false, '" + AccessLevel.RESTRICTED + "', 4, '" + RESTRICTED_PRODUCT + "')"
                );
    }

    @AfterEach
    void destroyDbState() {
        runSql("drop table geoservice_product_roles_view");
    }

    protected ResponseDefinitionBuilder successfulGeoserverResponse() {
        return aResponse()
                .withBody(GEOSERVER_RESPONSE_BODY)
                .withHeader(HttpHeaders.SERVER, GEOSERVER_SERVER_HEADER)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                .withStatus(200);
    }

    protected StubMapping mockGeoServer() {
        // mock geoserver using a http server returning http 200 for all GET-requests
        // https://stackoverflow.com/questions/74970761/how-do-i-write-a-unit-test-for-a-spring-cloud-reactive-api-gateways-custom-filt
        return stubFor(get(urlPathMatching("/geoserver/.*"))
                .willReturn(successfulGeoserverResponse())
        );
    }

    protected StubMapping mockGeoServerPost() {
        return stubFor(post(urlPathMatching("/geoserver/.*"))
                .willReturn(successfulGeoserverResponse())
        );
    }

    protected StubMapping mockGeoServerPostWithContentPattern(ContentPattern<?> bodyPattern) {
        return stubFor(post(urlPathMatching("/geoserver/.*"))
                .withRequestBody(bodyPattern)
                .willReturn(successfulGeoserverResponse())
        );
    }

    protected void expectGeoServerResponse(WebTestClient.ResponseSpec responseSpec) {
        responseSpec.expectHeader().valueEquals(HttpHeaders.SERVER, GEOSERVER_SERVER_HEADER);
        responseSpec.expectBody(String.class)
                .consumeWith(bodyContent -> Assertions.assertEquals(GEOSERVER_RESPONSE_BODY, bodyContent.getResponseBody()));
    }

    protected void expectNoGeoServerResponse(WebTestClient.ResponseSpec responseSpec) {
        responseSpec.expectHeader().doesNotExist(HttpHeaders.SERVER);
        responseSpec.expectBody(String.class)
                .consumeWith(bodyContent -> Assertions.assertNotEquals(GEOSERVER_RESPONSE_BODY, bodyContent.getResponseBody()));
    }

    protected void expectGeoServerResponseOk(WebTestClient.ResponseSpec responseSpec) {
        responseSpec.expectStatus().isOk();
        expectGeoServerResponse(responseSpec);
    }
}
