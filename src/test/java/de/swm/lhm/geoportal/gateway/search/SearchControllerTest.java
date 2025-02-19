package de.swm.lhm.geoportal.gateway.search;

import de.swm.lhm.geoportal.gateway.search.model.masterportal.PortalSearchRequest;
import de.swm.lhm.geoportal.gateway.search.model.masterportal.PortalSearchResponse;
import de.swm.lhm.geoportal.gateway.search.model.masterportal.SearchResultTo;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


class SearchControllerTest extends AbstractSearchControllerTest {
    public static String SEARCH_ENDPOINT = "/api/v1/search";
    PortalSearchRequest.PortalSearchRequestBuilder configuredSearchRequestBuilder = PortalSearchRequest.builder().
            searchString(SEARCH_STRING)
            .maxResultAmount(MAX_RESULT_AMOUNT)
            .searchOffset(5)
            .id("2323");

    @Test
    void publicAccess(){
        PortalSearchRequest publicSearchRequest = configuredSearchRequestBuilder
                .portalId(PUBLIC_PORTAL_ID)
                .build();
        WebTestClient.ResponseSpec response = postRequestWithoutGrants(publicSearchRequest);
        assureSuccessfulResponse(response);
    }

    @Test
    void accessProtected(){
        PortalSearchRequest protectedSearchRequest = configuredSearchRequestBuilder
                .portalId(PROTECTED_PORTAL_ID)
                .build();
        WebTestClient.ResponseSpec response = postRequestWithGrants(protectedSearchRequest, List.of(PROTECTED_PRODUCT), false);
        assureSuccessfulResponse(response);
    }

    @Test
    void accessProtectedAuthLevelHigh(){
        PortalSearchRequest protectedAuthLevelHighSearchRequest = configuredSearchRequestBuilder
                .portalId(PROTECTED_AUTH_LEVEL_HIGH_PORTAL_ID)
                .build();
        WebTestClient.ResponseSpec response = postRequestWithGrants(protectedAuthLevelHighSearchRequest, List.of(PROTECTED_AUTH_LEVEL_HIGH_PRODUCT), true);
        assureSuccessfulResponse(response);

    }


    @Test
    void whenAccessingNonExistingPortalThenGet404StatusCode(){
        PortalSearchRequest searchRequestNonExistingPortalId = configuredSearchRequestBuilder
                .portalId(NON_EXISTING_PORTAL_ID)
                .build();

        postRequestWithGrants(searchRequestNonExistingPortalId, List.of(), false)
                .expectStatus().isNotFound();

    }

    @Test
    void whenAccessingProtectedPortalWithWrongAuthenticationThenGet403StatusCode(){
        PortalSearchRequest searchRequestNonExistingPortalId = configuredSearchRequestBuilder
                .portalId(PROTECTED_PORTAL_ID)
                .build();

        postRequestWithGrants(searchRequestNonExistingPortalId, List.of(PROTECTED_AUTH_LEVEL_HIGH_PRODUCT), true)
                .expectStatus().isForbidden();

    }

    @Test
    void whenAccessingProtectedPortalWithoutAuthenticationThenRedirectToKeyCloak(){
        PortalSearchRequest protectedSearchRequest = PortalSearchRequest.builder()
                .portalId(PROTECTED_PORTAL_ID)
                .build();
        expectRedirectToKeyCloak(postRequestWithoutGrants(protectedSearchRequest));
    }

    @Test
    void whenAccessingProtectedAuthLevelHighPortalWithLowLevelAuthenticationThenGet403StatusCode(){
        PortalSearchRequest protectedAuthLevelHighSearchRequest = PortalSearchRequest.builder()
                .portalId(PROTECTED_AUTH_LEVEL_HIGH_PORTAL_ID)
                .build();

        postRequestWithGrants(protectedAuthLevelHighSearchRequest, List.of(PROTECTED_AUTH_LEVEL_HIGH_PRODUCT), false)
                .expectStatus().isForbidden();
    }


    private void assureSuccessfulResponse(WebTestClient.ResponseSpec responseSpec){
        responseSpec.expectStatus().isOk()
                .expectBody(PortalSearchResponse.class)
                .consumeWith(result -> {
                    List<SearchResultTo> searchResultList = result.getResponseBody().getHits().getHits();
                    assertThat(searchResultList).containsExactly(geoDataSearchResult, addressSearchResult);
                });
    }


    private WebTestClient.ResponseSpec postRequestWithGrants(PortalSearchRequest request, List<String> grantedProducts, boolean grantedAuthLevelHigh){
        return webTestClient.mutateWith(
                keyCloakConfigureGrantedProducts(grantedProducts, grantedAuthLevelHigh)
                ).post()
                .uri(SEARCH_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange();
    }

    private WebTestClient.ResponseSpec postRequestWithoutGrants(PortalSearchRequest request){
        return webTestClient
                .post()
                .uri(SEARCH_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange();
    }




}
