package de.swm.lhm.geoportal.gateway.print;

import de.swm.lhm.geoportal.gateway.print.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.List;
import java.util.stream.Stream;

class PrintTest extends AbstractPrintTest {

    String PRINT_URI = "/printserver/print/something/report.pdf";

    @ParameterizedTest
    @MethodSource("bodyWithPublicLayers")
    @DisplayName("Return Printserver response when printing public layers")
    void publicLayers(PrintRequest requestBody) {
        mockPrintServer();

        WebTestClient.ResponseSpec response = postRequestWithoutAuthentication(requestBody);

        response.expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(bodyContent -> Assertions.assertEquals(PRINT_SERVER_RESPONSE_BODY, bodyContent.getResponseBody()));

    }

    @ParameterizedTest
    @MethodSource("bodyWithProtectedLayers")
    @DisplayName("Redirect to keycloak when printing protected layers without authentication")
    void protectedLayerWithoutAuthentication(PrintRequest requestBody) {
        mockPrintServer();

        WebTestClient.ResponseSpec response = postRequestWithoutAuthentication(requestBody);

        expectRedirectToKeyCloak(response);

    }

    @ParameterizedTest
    @MethodSource("bodyWithProtectedLayers")
    @DisplayName("Return Printserver response when printing protected layer with right authentication")
    void protectedLayerWithRightAuthentication(PrintRequest requestBody) {
        mockPrintServer();

        WebTestClient.ResponseSpec response = postRequestWithAuthentication(requestBody, List.of(PROTECTED_PRODUCT), false);

        response.expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(bodyContent -> Assertions.assertEquals(PRINT_SERVER_RESPONSE_BODY, bodyContent.getResponseBody()));

    }

    @Test
    @DisplayName("Return Printserver response when printing high level auth protected layer with sufficient authentication")
    void protectedLayerHighLevelAuth() {
        mockPrintServer();

        PrintRequest requestBody = PrintRequest.builder().visibleLayerIds(List.of(PROTECTED_AUTH_LEVEL_HIGH_LAYER)).build();
        WebTestClient.ResponseSpec response = postRequestWithAuthentication(requestBody, List.of(PROTECTED_AUTH_LEVEL_HIGH_PRODUCT), true);

        response.expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(bodyContent -> Assertions.assertEquals(PRINT_SERVER_RESPONSE_BODY, bodyContent.getResponseBody()));
    }

    @Test
    @DisplayName("Return status forbidden when printing high level auth protected layer with low level authentication")
    void protectedLayerHighLevelAuthWithLowLevelAuthentication() {
        mockPrintServer();

        PrintRequest requestBody = PrintRequest.builder().visibleLayerIds(List.of(PROTECTED_AUTH_LEVEL_HIGH_LAYER)).build();
        WebTestClient.ResponseSpec response = postRequestWithAuthentication(requestBody, List.of(PROTECTED_AUTH_LEVEL_HIGH_PRODUCT), false);

        response.expectStatus().isForbidden();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/printserver/print/some_print_template/capabilities.json",
            "/printserver/print/some_print_template/status/a1-b2@c3.json",
            "/printserver/print/report/reference-id"
    })
    @DisplayName("get requests are not filtered and return printserver response")
    void allowGetRequests(String uri) {
        mockPrintServer();
        webTestClient
                .get()
                .uri(uri)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(bodyContent -> Assertions.assertEquals(PRINT_SERVER_RESPONSE_BODY, bodyContent.getResponseBody()));

    }


    private static Stream<PrintRequest> bodyWithPublicLayers() {
        return Stream.of(
                PrintRequest.builder().visibleLayerIds(List.of(PUBLIC_LAYER)).build(),
                createPrintServerRequestBody(List.of(), List.of(PUBLIC_LAYER), List.of(PUBLIC_LAYER2), List.of(), List.of()),
                createPrintServerRequestBody(List.of(), List.of(), List.of(), List.of(PUBLIC_LAYER), List.of(PUBLIC_LAYER2)),
                createPrintServerRequestBody(List.of(), List.of(PUBLIC_LAYER, PUBLIC_LAYER2), List.of(), List.of(), List.of())
        );
    }

    private static Stream<PrintRequest> bodyWithProtectedLayers() {
        return Stream.of(
                PrintRequest.builder().visibleLayerIds(List.of(PROTECTED_LAYER)).build(),
                PrintRequest.builder().visibleLayerIds(List.of(PUBLIC_LAYER, PROTECTED_LAYER)).build(),
                createPrintServerRequestBody(List.of(PUBLIC_LAYER), List.of(PROTECTED_LAYER), List.of(), List.of(), List.of())
        );
    }


    private WebTestClient.ResponseSpec postRequestWithoutAuthentication(PrintRequest requestBody) {
        return webTestClient
                .post()
                .uri(PRINT_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .exchange();
    }

    private WebTestClient.ResponseSpec postRequestWithAuthentication(PrintRequest requestBody, List<String> grantedProducts, boolean grantedAuthLevelHigh) {
        return webTestClient
                .mutateWith(
                        keyCloakConfigureGrantedProducts(grantedProducts, grantedAuthLevelHigh)
                )
                .post()
                .uri(PRINT_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .exchange();
    }


    private static PrintRequest createPrintServerRequestBody(List<String> visibleLayers, List<String> mapLayers, List<String> mapLayer2, List<String> gfiLayers, List<String> legendLayers) {
        return PrintRequest.builder().visibleLayerIds(visibleLayers).attributes(
                Attributes.builder().map(
                        Map.builder().layers(
                                List.of(LayersItem.builder().layerName("mapLayer").layers(mapLayers).build(),
                                        LayersItem.builder().layerName("mapLayer2").layers(mapLayer2).build())
                        ).build()
                ).gfi(
                        Gfi.builder().layers(
                                List.of(LayersItem.builder().layerName("gifLayer").layers(gfiLayers).build())
                        ).build()
                ).legend(
                        Legend.builder().layers(
                                List.of(LayersItem.builder().layerName("legendLayer").layers(legendLayers).build())
                        ).build()
                ).build()
        ).build();
    }


}
