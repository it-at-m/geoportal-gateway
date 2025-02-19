package de.swm.lhm.geoportal.gateway.actuator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


class ActuatorTest extends BaseIntegrationTest {

    @Test
    void testActuator() throws JsonProcessingException {

        byte[] bytes = webTestClient.get().uri("/actuator/info")
                .exchange()
                .expectStatus().isOk()
                .expectBody().returnResult().getResponseBody();
        assert bytes != null;
        String json = new String(bytes, StandardCharsets.UTF_8);
        Map<String, Map<String, String>> parsed = new ObjectMapper()
                .readValue(json, new TypeReference<>() {});

        assertThat(parsed, allOf(aMapWithSize(2), hasKey("app"),  hasKey("app")));

        Map<String, String> app = parsed.get("app");
        assertThat(app, allOf(aMapWithSize(2), hasKey("name"),  hasKey("id")));

        Map<String, String> git = parsed.get("git");
        assertThat(git, allOf(aMapWithSize(3), hasKey("version"),  hasKey("id"),  hasKey("time")));

    }

}