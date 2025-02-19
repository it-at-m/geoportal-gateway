package de.swm.lhm.geoportal.gateway.icon.serve;

import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.util.stream.Stream;

@TestPropertySource(properties = {"geoportal.style.icon.endpoint=" + IconControllerTest.ICON_ENDPOINT})
public class IconControllerTest extends BaseIntegrationTest {
    public static final String ICON_ENDPOINT = "/resource/icon";

    @ParameterizedTest
    @MethodSource("provideIconPathAndMediaType")
    void serveIconCorrectly(String filePath, MediaType mediaType) {
        webTestClient.get()
                .uri(ICON_ENDPOINT + filePath)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(mediaType);
    }

    private static Stream<Arguments> provideIconPathAndMediaType() {
        return Stream.of(
                Arguments.of("/plan/icons/3W1D2P/RYTFJQMLF/icon_W346Q58VSR.jpg", MediaType.IMAGE_JPEG),
                Arguments.of("/gsm/icons/icon_4S8VTPR39B.png", MediaType.IMAGE_PNG),
                Arguments.of("/plan/icons/icon_L953P3B7.svg", MediaType.valueOf("image/svg+xml"))
        );
    }

    @Test
    void returnNotFoundWhenFileDoesNotExist() {
        webTestClient.get()
                .uri(ICON_ENDPOINT + "/non-existing/path")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void returnForbiddenWhenTryingToAccessDifferentFolder() {
        webTestClient.get()
                .uri(ICON_ENDPOINT + "/../../application-test.properties")
                .exchange()
                .expectStatus().isForbidden();
    }
}