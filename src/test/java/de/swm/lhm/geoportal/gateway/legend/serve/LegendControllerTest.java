package de.swm.lhm.geoportal.gateway.legend.serve;

import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.util.stream.Stream;

@TestPropertySource(properties = {"geoportal.style.legend.endpoint=" + LegendControllerTest.LEGEND_ENDPOINT})
public class LegendControllerTest extends BaseIntegrationTest {
    public static final String LEGEND_ENDPOINT = "/resource/legend";

    @ParameterizedTest
    @MethodSource("provideLegendPathAndMediaType")
    void serveLegendCorrectly(String filePath, MediaType mediaType) {
        webTestClient.get()
                .uri(LEGEND_ENDPOINT + filePath)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(mediaType);
    }

    private static Stream<Arguments> provideLegendPathAndMediaType() {
        return Stream.of(
                Arguments.of("/gsm/legends/86FZJ/legend_DOZGWX.jpeg", MediaType.IMAGE_JPEG),
                Arguments.of("/plan/legends/legend_8TMJ7H.png", MediaType.IMAGE_PNG)
        );
    }

    @Test
    void returnNotFoundWhenFileDoesNotExist() {
        webTestClient.get()
                .uri(LEGEND_ENDPOINT + "/non-existing/path")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void returnForbiddenWhenTryingToAccessDifferentFolder() {
        webTestClient.get()
                .uri(LEGEND_ENDPOINT + "/../../application-test.properties")
                .exchange()
                .expectStatus().isForbidden();
    }
}
