package de.swm.lhm.geoportal.gateway.print;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import de.swm.lhm.geoportal.gateway.authorization.model.AccessLevel;
import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@AutoConfigureWireMock(port = 0) // random port
@TestPropertySource(properties = {
        "geoportal.gateway.load-balancer.services.mapfish.urls[0]=http://localhost:" + "${wiremock.server.port}",
})
public class AbstractPrintTest extends BaseIntegrationTest {
    protected final static String PUBLIC_LAYER = "myws:publiclayer";
    protected final static String PUBLIC_LAYER2 = "myws:publiclayer2";
    protected final static String PROTECTED_LAYER = "myws:protectedlayer";
    protected final static String PROTECTED_LAYER2 = "myws:protectedlayer2";
    protected final static String PROTECTED_AUTH_LEVEL_HIGH_LAYER = "myws:protectedhighlayer";
    protected final static String RESTRICTED_LAYER = "myws:restrictedlayer";
    protected final static String PRINT_SERVER_RESPONSE_BODY = "beep";

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
                        "('" + PUBLIC_LAYER2 + "', '" + Stage.CONFIGURATION + "', false, '" + AccessLevel.PUBLIC + "', 6, '" + PUBLIC_PRODUCT + "')," +
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

    protected ResponseDefinitionBuilder successfulPrintServerResponse() {
        return aResponse()
                .withBody(PRINT_SERVER_RESPONSE_BODY)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                .withStatus(200);
    }

    protected void mockPrintServer() {
        stubFor(post(urlPathMatching("/printserver/print/.*"))
                .willReturn(successfulPrintServerResponse()));
        stubFor(get(urlPathMatching("/printserver/print/.*"))
                .willReturn(successfulPrintServerResponse()));
    }
}
