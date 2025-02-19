package de.swm.lhm.geoportal.gateway.sensor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


@Import(SensorRoutesCollectorTestConfig.class)
@AutoConfigureWireMock(port = SensorRoutesCollectorTest.FROST_PORT)
@TestPropertySource(properties = {
        "geoportal.gateway.sensor.routes.enabled=true"
})

@Slf4j
class SensorRoutesCollectorTest extends BaseIntegrationTest {

    public static final int FROST_PORT = 12345;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void testSensorRoutes() throws JsonProcessingException {

        List<Map<String, Object>> routes = getRoutes();

        testRoute(
                routes,
                "2e493acd-1551-4bcb-b8bb-b96f05290b87",
                "/sensor/2e493acd-1551-4bcb-b8bb-b96f05290b87/**",
                "http://localhost:12345/frost"
        );

        stubFor(get(urlPathMatching("/frost/first/.*"))
                .willReturn(
                        aResponse()
                                .withBody("beep")
                                .withHeader(HttpHeaders.SERVER, "frost")
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                                .withStatus(200)
                ));

        webTestClient.get()
                .uri("/sensor/2e493acd-1551-4bcb-b8bb-b96f05290b87/first/one")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.SERVER, "frost")
                .expectBody(String.class).consumeWith(
                        bodyContent -> assertThat(bodyContent.getResponseBody(), is("beep")));
    }

    @Test
    void testSensorRoutesReload() throws JsonProcessingException {

        List<Map<String, Object>> routes = getRoutes();

        testRoute(
                routes,
                "2e493acd-1551-4bcb-b8bb-b96f05290b87",
                "/sensor/2e493acd-1551-4bcb-b8bb-b96f05290b87/**",
                "http://localhost:12345/frost"
        );

        runSql("""
                DELETE FROM t_plugin_sta_sensor;
                
                INSERT INTO t_plugin_sta_sensor
                    (id, name, url, stageless_id, stage)
                VALUES
                    ('8f06cc57-64a6-4513-86fc-c256b37045ee', 'layer1', 'http://localhost:8091/frost', '3ce998f0-a61d-4504-aa49-fdf22baebc5b', 'CONFIGURATION');
                """);

        webTestClient.post()
                .uri( "/api/v1/routes/refresh").exchange()
                .expectStatus().isOk();

        routes = getRoutes();

        testRoute(
                routes,
                "8f06cc57-64a6-4513-86fc-c256b37045ee",
                "/sensor/8f06cc57-64a6-4513-86fc-c256b37045ee/**",
                "http://localhost:8091/frost"
        );

    }

    private List<Map<String, Object>> getRoutes() throws JsonProcessingException {

        List<String> rawResponse = webTestClient.get()
                .uri("/actuator/gateway/routes")
                .exchange()
                .returnResult(String.class)
                .getResponseBody().collectList().block();

        assertThat(rawResponse, is(not(nullValue())));

        String response = String.join("", rawResponse);

        log.debug("actuator returned: {}", response);

        List<Map<String, Object>> routes = objectMapper.readValue(response, new TypeReference<>(){});

        assertThat(routes, is(not(nullValue())));

        return routes;

    }

    private void testRoute(
            List<Map<String, Object>> routes,
            String routeId,
            String predicate,
            String uri
    ) {
        Optional<Map<String, Object>> result = routes.stream()
                .filter(map -> map.containsKey("route_id"))
                .filter(map -> map.get("route_id").equals(routeId))
                .findFirst();

        assertThat(result.isPresent(), is(true));

        assertThat((String) result.get().getOrDefault("predicate", "nope"), containsString(predicate));
        assertThat((String) result.get().getOrDefault("uri", "nope"), is(uri));

        Object filtersRaw = result.get().getOrDefault("filters", List.of());
        assertThat(filtersRaw, instanceOf(List.class));

        List<?> filtersList = (List<?>) result.get().getOrDefault("filters", List.of());
        assertThat(filtersList, hasSize(4));
        assertThat(filtersList.getFirst(), instanceOf(String.class));
        assertThat(filtersList.get(1), instanceOf(String.class));

        List<String> filters = (List<String>) filtersList;

        assertThat(filters, hasItems(containsString("StripPrefix"), containsString("parts"), containsString("2")));
        assertThat(filters, hasItems(containsString("PrefixPath"), containsString("/frost")));
    }

}