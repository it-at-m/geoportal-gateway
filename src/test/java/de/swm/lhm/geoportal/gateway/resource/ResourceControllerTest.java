package de.swm.lhm.geoportal.gateway.resource;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ContentDisposition;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@TestPropertySource(properties = {
        "geoportal.resource.enable-webserver=false",
        "geoportal.resource.local-path=src/test/resources/de/swm/lhm/geoportal/gateway/resource/files",
        "geoportal.resource.endpoint=" + ResourceControllerTest.RESOURCE_ENDPOINT
})
public class ResourceControllerTest  extends AbstractResourceControllerTest {

    public static final String RESOURCE_ENDPOINT = "/resource/download";
    public static final String DOKUMENTE = "dokumente";

    @Test
    void whenAccessingResourceThenExpectBodyWithDocument() {
        webTestClient.get()
                .uri(RESOURCE_ENDPOINT + "/brand/dokumente/text1.txt")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentDisposition(ContentDisposition.builder("attachment").filename("text1.txt").build())
                .expectBody(String.class)
                .value(content -> {
                    assertThat(content).contains("Test");
                });
    }

    @Test
    void returnNotFoundWhenFileDoesNotExist() {
        webTestClient.get()
                .uri(RESOURCE_ENDPOINT + "/non-existing/path")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void returnForbiddenWhenTryingToAccessDifferentFolder() {
        webTestClient.get()
                .uri(RESOURCE_ENDPOINT + "/../../application-test.properties")
                .exchange()
                .expectStatus().isForbidden();
    }

    @ParameterizedTest
    @MethodSource("providePublicResources")
    void publicAccess(String unit, String layerName, String fileName) {
        webTestClient.get()
                .uri(RESOURCE_ENDPOINT + constructPath(unit, layerName, fileName))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentDisposition(ContentDisposition.builder("attachment").filename(fileName).build());
    }

    @ParameterizedTest
    @MethodSource("provideProtectedResources")
    void accessProtected(String unit, String layerName, String fileName){
        webTestClient.mutateWith(
                keyCloakConfigureGrantedProducts(List.of(PROTECTED_PRODUCT), false)
        ).get()
                .uri( RESOURCE_ENDPOINT + constructPath(unit, layerName, fileName))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentDisposition(ContentDisposition.builder("attachment").filename(fileName).build());
    }

    @ParameterizedTest
    @MethodSource("provideProtectedAuthLevelHighResources")
    void accessProtectedAuthLevelHigh(String unit, String layerName, String fileName){
        webTestClient.mutateWith(
                        keyCloakConfigureGrantedProducts(List.of(PROTECTED_AUTH_LEVEL_HIGH_PRODUCT), true)
                ).get()
                .uri( RESOURCE_ENDPOINT + constructPath(unit, layerName, fileName))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentDisposition(ContentDisposition.builder("attachment").filename(fileName).build());
    }

    @ParameterizedTest
    @MethodSource("provideProtectedAndProtectedAuthLevelHighCombinedResources")
    void accessProtectedAndProtectedAuthLevelHighCombined(String unit, String layerName, String fileName){
        webTestClient.mutateWith(
                        keyCloakConfigureGrantedProducts(List.of(PROTECTED_AUTH_LEVEL_HIGH_PRODUCT, PROTECTED_PRODUCT), true)
                ).get()
                .uri( RESOURCE_ENDPOINT + constructPath(unit, layerName, fileName))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentDisposition(ContentDisposition.builder("attachment").filename(fileName).build());
    }

    @ParameterizedTest
    @MethodSource("provideProtectedAuthLevelHighResources")
    void whenAccessingProtectedAuthLevelHighResourcesWithLowLevelAuthenticationThenGet403StatusCode(String unit, String layerName, String fileName){
        webTestClient.mutateWith(
                        keyCloakConfigureGrantedProducts(List.of(PROTECTED_AUTH_LEVEL_HIGH_PRODUCT), false)
                ).get()
                .uri( RESOURCE_ENDPOINT + constructPath(unit, layerName, fileName))
                .exchange()
                .expectStatus().isForbidden();
    }

    @ParameterizedTest
    @MethodSource("provideProtectedResources")
    void whenAccessingProtectedResourcesWithoutAuthenticationThenRedirectToKeyCloak(String unit, String layerName, String fileName){
        WebTestClient.ResponseSpec res = webTestClient.get()
                .uri( RESOURCE_ENDPOINT + constructPath(unit, layerName, fileName))
                .exchange();
        expectRedirectToKeyCloak(res);

    }

    @ParameterizedTest
    @MethodSource("provideProtectedResources")
    void whenAccessingProtectedResourcesWithWrongAuthenticationThenGet403StatusCode(String unit, String layerName, String fileName){
        webTestClient.mutateWith(
                        keyCloakConfigureGrantedProducts(List.of(PROTECTED_PRODUCT2), false)
                ).get()
                .uri( RESOURCE_ENDPOINT + constructPath(unit, layerName, fileName))
                .exchange()
                .expectStatus().isForbidden();
    }



    private static Stream<Arguments> providePublicResources() {
        // resources which are public or not configured and associated to a public or unknown layer or no layer
        return Stream.of(
                Arguments.of(NO_LAYER_UNIT, null, PUBLIC_CONFIGURED_RESOURCE),
                Arguments.of(NO_LAYER_UNIT, null, NOT_CONFIGURED_RESOURCE),
                Arguments.of(STANDARD_UNIT, PUBLIC_LAYER_NAME, PUBLIC_CONFIGURED_RESOURCE),
                Arguments.of(STANDARD_UNIT, PUBLIC_LAYER_NAME, NOT_CONFIGURED_RESOURCE),
                Arguments.of(STANDARD_UNIT, UNKNOWN_LAYER_NAME, PUBLIC_CONFIGURED_RESOURCE),
                Arguments.of(STANDARD_UNIT, UNKNOWN_LAYER_NAME, NOT_CONFIGURED_RESOURCE));
    }

    private static Stream<Arguments> provideProtectedResources() {
        // resources which are associated to a protected layer or are configured to be protected
        return Stream.of(
                Arguments.of(NO_LAYER_UNIT, null, PROTECTED_CONFIGURED_RESOURCE),
                Arguments.of(STANDARD_UNIT, PUBLIC_LAYER_NAME, PROTECTED_CONFIGURED_RESOURCE),
                Arguments.of(STANDARD_UNIT, UNKNOWN_LAYER_NAME, PROTECTED_CONFIGURED_RESOURCE),
                Arguments.of(STANDARD_UNIT, PROTECTED_LAYER_NAME, PROTECTED_CONFIGURED_RESOURCE),
                Arguments.of(STANDARD_UNIT, PROTECTED_LAYER_NAME, PUBLIC_CONFIGURED_RESOURCE),
                Arguments.of(STANDARD_UNIT, PROTECTED_LAYER_NAME, NOT_CONFIGURED_RESOURCE)
        );
    }

    public static Stream<Arguments> provideProtectedAuthLevelHighResources(){
        return Stream.of(
                Arguments.of(STANDARD_UNIT, PROTECTED_AUTH_LEVEL_HIGH_LAYER_NAME, PUBLIC_CONFIGURED_RESOURCE),
                Arguments.of(STANDARD_UNIT, PROTECTED_AUTH_LEVEL_HIGH_LAYER_NAME, NOT_CONFIGURED_RESOURCE)
        );
    }

    public static Stream<Arguments> provideProtectedAndProtectedAuthLevelHighCombinedResources(){
        return Stream.of(
                Arguments.of(STANDARD_UNIT, PROTECTED_AUTH_LEVEL_HIGH_LAYER_NAME, PROTECTED_CONFIGURED_RESOURCE)
                );
    }


    private static String constructPath(String unit, String layer, String fileName){
        if (layer == null){
            return "/" + unit + "/" + DOKUMENTE + "/" + fileName;
        } else {
            return "/" + unit + "/" + DOKUMENTE + "/" + layer + "/" + fileName;
        }
    }








}
