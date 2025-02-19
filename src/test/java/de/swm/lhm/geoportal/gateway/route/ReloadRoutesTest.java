package de.swm.lhm.geoportal.gateway.route;

import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static de.swm.lhm.geoportal.gateway.route.MockRouteCollectorConfig.getCurrentRoute;
import static de.swm.lhm.geoportal.gateway.route.MockRouteCollectorConfig.getOldRoutes;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


@Import(MockRouteCollectorConfig.class)
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ReloadRoutesTest extends BaseIntegrationTest {

    @Autowired
    MockRouteCollectorConfig mockRouteCollectorConfig;

    @Test
    void reloadRoutesTest() {

        assertThatHasConfiguredRoutes();

        List<String> refreshResponse = webTestClient.post()
                .uri( "/api/v1/routes/refresh").exchange()
                .expectStatus().isOk()
                .returnResult(String.class).getResponseBody().collectList().block();

        assertThat(refreshResponse, hasSize(1));
        assertThat(refreshResponse.getFirst(), is(GatewayRoutesController.RESPONSE_MESSAGE));

        assertThatHasConfiguredRoutes();

    }

    private void assertThatHasConfiguredRoutes(){
        List<String> rawResponse = webTestClient.get()
                .uri("/actuator/gateway/routes")
                .exchange()
                .returnResult(String.class).getResponseBody().collectList().block();

        assertThat(rawResponse, is(not(nullValue())));

        String response = String.join("", rawResponse);

        log.debug("{}", response);

        assertThat(response, containsString(getCurrentRoute().getPath()));
        assertThat(response, containsString(getCurrentRoute().getUrl()));

        getOldRoutes()
                .forEach(route -> {
                    assertThat(response, not(containsString(route.getPath())));
                    assertThat(response, not(containsString(route.getUrl())));
                });

    }

}




