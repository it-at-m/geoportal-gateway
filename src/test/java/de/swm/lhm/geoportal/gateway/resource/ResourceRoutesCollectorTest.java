package de.swm.lhm.geoportal.gateway.resource;


import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@AutoConfigureWireMock(port = 0) // random port
@Slf4j
@TestPropertySource(properties = {
        "geoportal.resource.enable-webserver=true",
        "geoportal.resource.webserver-path=http://localhost:" + "${wiremock.server.port}" + ResourceRoutesCollectorTest.WEBSERVER_RESOURCES_PATH,
        "geoportal.resource.endpoint=" + ResourceControllerTest.RESOURCE_ENDPOINT
})
public class ResourceRoutesCollectorTest  extends AbstractResourceControllerTest {
    @Autowired
    RouteLocator routeLocator;

    public static final String WEBSERVER_RESOURCES_PATH = "/path/to/resources";

    public static final String RESOURCE_ENDPOINT = "/resource/download";
    private static final String TEXT_1_PATH = "/gsm/dokumente/wonderful_text.txt";
    private static final String PDF_1_PATH = "/dir/dokumente/pdf1.pdf";

    @BeforeEach
    void mockWebServer(){
        stubFor(get(urlPathMatching(WEBSERVER_RESOURCES_PATH + TEXT_1_PATH))
                .willReturn(ok()
                        .withHeader("Content-Type", MediaType.TEXT_PLAIN_VALUE)));

        stubFor(get(urlPathMatching(WEBSERVER_RESOURCES_PATH + PDF_1_PATH))
                .willReturn(ok()
                        .withHeader("Content-Type", MediaType.APPLICATION_PDF_VALUE)));
    }

    @ParameterizedTest
    @MethodSource("provideResourcePathAndMediaType")
    void serveResourceCorrectlyViaWebserver(String filePath, MediaType mediaType) {
        webTestClient.get()
                .uri(RESOURCE_ENDPOINT + filePath)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(mediaType);
    }

    private static Stream<Arguments> provideResourcePathAndMediaType() {
        return Stream.of(
                Arguments.of(TEXT_1_PATH, MediaType.TEXT_PLAIN),
                Arguments.of(PDF_1_PATH, MediaType.APPLICATION_PDF)
        );
    }
}
