package de.swm.lhm.geoportal.gateway.search.client;


import com.github.tomakehurst.wiremock.WireMockServer;
import com.tngtech.keycloakmock.api.ServerConfig;
import com.tngtech.keycloakmock.junit5.KeycloakMockExtension;
import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import de.swm.lhm.geoportal.gateway.search.model.elastic.AddressDocument;
import de.swm.lhm.geoportal.gateway.search.model.elastic.ElasticSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@ActiveProfiles("elasticsearch")
class ElasticSearchClientTest extends BaseIntegrationTest {

    private static final List<Integer> elasticSearchPorts = List.of(8090, 8091,8092);
    private static final String USER_NAME = "username";
    private static final String PASSWORD = "password";

    @Autowired
    private ElasticSearchClient client;

    @RegisterExtension
    static KeycloakMockExtension mockKeyCloak = new KeycloakMockExtension(ServerConfig
            .aServerConfig()
            .withNoContextPath()
            .withPort(BaseIntegrationTest.KEYCLOAK_PORT)
            .withDefaultRealm("public")
            .build());

    @BeforeEach
    void createMockElasticSearchInstances() {
        for (int instance = 0; instance < elasticSearchPorts.size(); instance++) {

            WireMockServer wireMockServer = new WireMockServer(elasticSearchPorts.get(instance));
            wireMockServer.start();

            configureFor("localhost", elasticSearchPorts.get(instance));
            stubFor(post(urlPathMatching("/addresses/_search"))
                    .withBasicAuth(USER_NAME, PASSWORD)
                    .withHeader("accept", containing(APPLICATION_JSON_VALUE))
                    .withHeader("content-type", equalTo(APPLICATION_JSON_VALUE))
                    .willReturn(
                            ok().withHeader("Content-Type", APPLICATION_JSON_VALUE)
                                    .withBody("{ \"took\": " + instance + " }")));
        }
    }

    @Test
    void testRoundRobinLoadBalancingAndHeadersDefinedByElasticSearchClientConfig(){
        List<Integer> used_instances = new ArrayList<>();
        List<Integer> expected_instances = new ArrayList<>();
        for (int instance = 0; instance < elasticSearchPorts.size(); instance++) {
            ElasticSearchResponse<AddressDocument> response = client.searchAddress("searchString", 5).block();
            used_instances.add(response.getTook());
            expected_instances.add(instance);
        }
        assertThat(used_instances).containsAll(expected_instances);

    }

}
