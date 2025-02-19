package de.swm.lhm.geoportal.gateway.search;

import de.swm.lhm.geoportal.gateway.portal.PortalRepository;
import de.swm.lhm.geoportal.gateway.search.client.ElasticSearchClient;
import de.swm.lhm.geoportal.gateway.search.model.elastic.ElasticSearchInnerHit;
import de.swm.lhm.geoportal.gateway.search.model.elastic.ElasticSearchOuterHits;
import de.swm.lhm.geoportal.gateway.search.model.elastic.ElasticSearchResponse;
import de.swm.lhm.geoportal.gateway.search.model.mapper.AddressSearchResultMapper;
import de.swm.lhm.geoportal.gateway.search.model.mapper.GeoDataSearchResultMapper;
import de.swm.lhm.geoportal.gateway.search.model.mapper.ResultMapper;
import de.swm.lhm.geoportal.gateway.search.model.masterportal.PortalSearchOuterHits;
import de.swm.lhm.geoportal.gateway.search.model.masterportal.PortalSearchRequest;
import de.swm.lhm.geoportal.gateway.search.model.masterportal.PortalSearchResponse;
import de.swm.lhm.geoportal.gateway.search.model.masterportal.SearchResultTo;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.NoSuchElementException;

@Service
@AllArgsConstructor
public class SearchService {
    private final ElasticSearchClient client;
    private final GeoDataSearchResultMapper geoDataSearchResultMapper;
    private final AddressSearchResultMapper addressSearchResultMapper;
    private final PortalRepository portalRepository;
    private final GatewayService gatewayService;


    public Mono<PortalSearchResponse> executePortalSearch(PortalSearchRequest request){
        Flux<SearchResultTo> geoData = portalRepository.findPortalByIdAndStage(request.getPortalId(), gatewayService.getStage())
                .switchIfEmpty(Mono.error(new NoSuchElementException("Portal with id " +  request.getPortalId() + " not found")))
                .flatMapMany(portal -> searchGeoData(request.getSearchString(), portal.getSearchIndexGeoData()));

        Flux<SearchResultTo> addresses = searchAddress(request.getSearchString(), request.getMaxResultAmount());
        Flux<SearchResultTo> results = geoData.concatWith(addresses);
        return results.collectList()
                .map(PortalSearchOuterHits::new)
                .map(PortalSearchResponse::new);
    }


    public Flux<SearchResultTo> searchGeoData(String searchString, String index){
        return mapToSearchResult(client.searchGeoData(index, searchString), geoDataSearchResultMapper);
    }


    public Flux<SearchResultTo> searchAddress(String searchString, int maxResultAmount){
        return mapToSearchResult(client.searchAddress(searchString, maxResultAmount), addressSearchResultMapper);
    }

    private <T> Flux<SearchResultTo> mapToSearchResult(Mono<ElasticSearchResponse<T>> clientResponse, ResultMapper<T> mapper){
        return clientResponse.map(ElasticSearchResponse::getHits)
                .map(ElasticSearchOuterHits::getHits)
                .flatMapMany(elasticsearchInnerHits ->
                        Flux.fromIterable(elasticsearchInnerHits)
                            .map(ElasticSearchInnerHit::get_source)
                            .map(mapper::map));
    }
}
