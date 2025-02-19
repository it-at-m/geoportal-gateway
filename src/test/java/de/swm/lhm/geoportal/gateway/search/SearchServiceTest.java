package de.swm.lhm.geoportal.gateway.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.swm.lhm.geoportal.gateway.portal.PortalRepository;
import de.swm.lhm.geoportal.gateway.search.client.ElasticSearchClient;
import de.swm.lhm.geoportal.gateway.search.client.ElasticSearchQueryCreator;
import de.swm.lhm.geoportal.gateway.search.model.mapper.AddressSearchResultMapper;
import de.swm.lhm.geoportal.gateway.search.model.mapper.AddressSearchResultMapperImpl;
import de.swm.lhm.geoportal.gateway.search.model.mapper.GeoDataSearchResultMapper;
import de.swm.lhm.geoportal.gateway.search.model.mapper.GeoDataSearchResultMapperImpl;
import de.swm.lhm.geoportal.gateway.search.model.masterportal.SearchResultTo;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@Import({GeoDataSearchResultMapperImpl.class, AddressSearchResultMapperImpl.class, ElasticSearchQueryCreator.class, ObjectMapper.class,})
class SearchServiceTest {
    @Mock
    private ExchangeFunction exchangeFunction;
    @Autowired
    private GeoDataSearchResultMapper geoDataSearchResultMapper;
    @Autowired
    private AddressSearchResultMapper addressSearchResultMapper;
    @Autowired
    private  ElasticSearchQueryCreator elasticSearchQueryCreator;
    @Mock
    private PortalRepository portalRepository;
    @Mock
    private GatewayService gatewayService;
    private SearchService searchService;

    public SearchServiceTest() {
    }

    @BeforeEach
    void init(){
        WebClient clientMock = WebClient.builder()
                .exchangeFunction(exchangeFunction)
                .build();
        searchService = new SearchService(new ElasticSearchClient(clientMock, elasticSearchQueryCreator, new ElasticSearchProperties()), geoDataSearchResultMapper, addressSearchResultMapper, portalRepository, gatewayService);
    }


    @Test
    void testSearchGeoData() throws IOException{
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(geoDataResponse());
        Flux<SearchResultTo> res = searchService.searchGeoData("searchString", "geoDataIndex");
        SearchResultTo expected = SearchResultTo.builder().id("d4914374-784b-4750-9a17-ca054197e11f")
                .type("Geodaten")
                .coordinate(List.of(5333536.976132349, 687716.6843805218))
                .displayValue("Parkhaus Audi Dome (opendata_parkhaeuser)")
                .layerId("764197")
                .layerTitle("opendata_parkhaeuser")
                .geoDataValue("Parkhaus Audi Dome")
                .build();

        StepVerifier.create(res)
                .expectNextMatches(hit -> hit.equals(expected))
                .expectComplete()
                .verify();
    }

    private Mono<ClientResponse> geoDataResponse() throws IOException {
        String jsonBody = new String(Files.readAllBytes(Paths.get("src/test/resources/search/elastic_search_response_geo_data.json")), StandardCharsets.UTF_8);
        return Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("content-type", "application/json")
                .body(jsonBody)
                .build());
    }

    @Test
    void testSearchAddress() throws IOException{
        when(exchangeFunction.exchange(any())).thenReturn(getAddressResponse());
        Flux<SearchResultTo> res = searchService.searchAddress("searchString", 10);
        SearchResultTo firstHitExpected = SearchResultTo.builder().id("fdc53678-0cef-4349-a662-a1d49dd49ee2")
                .type("Adresse")
                .id("fdc53678-0cef-4349-a662-a1d49dd49ee2")
                .coordinate(List.of(5333874.0, 691413.0))
                .displayValue("Klenzestraße 59, 80469 München")
                .streetName("Klenzestraße")
                .city("München")
                .zipCode("80469")
                .streetNameComplete("Klenzestraße 59")
                .build();

        StepVerifier.create(res)
                .expectNextMatches(hit -> hit.equals(firstHitExpected))
                .expectNextMatches(hit -> hit.getStreetNameComplete().equals("Klenzestraße 73"))
                .expectComplete()
                .verify();
    }

    private Mono<ClientResponse> getAddressResponse() throws IOException{
        String jsonBody = new String(Files.readAllBytes(Paths.get("src/test/resources/search/elastic_search_response_address.json")), StandardCharsets.UTF_8);
        return Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("content-type", "application/json")
                .body(jsonBody)
                .build());
    }
}
