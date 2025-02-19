package de.swm.lhm.geoportal.gateway.search.client;

import de.swm.lhm.geoportal.gateway.search.ElasticSearchProperties;
import de.swm.lhm.geoportal.gateway.search.model.elastic.AddressDocument;
import de.swm.lhm.geoportal.gateway.search.model.elastic.ElasticSearchResponse;
import de.swm.lhm.geoportal.gateway.search.model.elastic.GeoDataDocument;
import lombok.AllArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static de.swm.lhm.geoportal.gateway.search.ElasticSearchProperties.SERVICE_ID;

@Component
@AllArgsConstructor
public class ElasticSearchClient {

    private final WebClient client;
    private final ElasticSearchQueryCreator elasticSearchQueryCreator;
    private final ElasticSearchProperties elasticSearchProperties;

    public Mono<ElasticSearchResponse<GeoDataDocument>> searchGeoData(String index, String searchString) {
        String query = elasticSearchQueryCreator.createGeoDataQuery(searchString);
        return postQuery(index, query)
                .bodyToMono(new ParameterizedTypeReference<>() {
        });
    }

    public Mono<ElasticSearchResponse<AddressDocument>> searchAddress(String searchString, int maxResultAmount){
        String query = elasticSearchQueryCreator.createAddressQuery(searchString, maxResultAmount);
        return postQuery(elasticSearchProperties.getAddressIndexName(), query)
                .bodyToMono(new ParameterizedTypeReference<>() {
        });
    }

    private WebClient.ResponseSpec postQuery(String index, String query) {
        return client.post()
                .uri("http://{service-id}/{index}/_search", SERVICE_ID, index)
                .bodyValue(query)
                .retrieve();
    }
}
