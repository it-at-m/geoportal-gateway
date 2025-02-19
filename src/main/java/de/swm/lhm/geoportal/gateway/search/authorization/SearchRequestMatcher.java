package de.swm.lhm.geoportal.gateway.search.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.swm.lhm.geoportal.gateway.authorization.IAuthService;
import de.swm.lhm.geoportal.gateway.portal.PortalRepository;
import de.swm.lhm.geoportal.gateway.portal.PortalService;
import de.swm.lhm.geoportal.gateway.portal.authorization.PortalRequestMatcher;
import de.swm.lhm.geoportal.gateway.portal.model.Portal;
import de.swm.lhm.geoportal.gateway.search.SearchProperties;
import de.swm.lhm.geoportal.gateway.search.model.masterportal.PortalSearchRequest;
import de.swm.lhm.geoportal.gateway.shared.GatewayService;
import de.swm.lhm.geoportal.gateway.util.DataBufferUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Setter
@Getter
@Slf4j
public class SearchRequestMatcher extends PortalRequestMatcher {

    private final IAuthService authorizationService;
    private final PortalRepository portalRepository;
    private final GatewayService gatewayService;
    private final ObjectMapper objectMapper;
    private final SearchProperties searchProperties;

    public SearchRequestMatcher(IAuthService authorizationService,
                                PortalService portalService,
                                PortalRepository portalRepository,
                                GatewayService gatewayService,
                                ObjectMapper objectMapper,
                                SearchProperties searchProperties) {
        super(authorizationService, portalService);
        this.authorizationService = authorizationService;
        this.portalRepository = portalRepository;
        this.gatewayService = gatewayService;
        this.objectMapper = objectMapper;
        this.searchProperties = searchProperties;
    }


    @Override
    public Mono<MatchResult> matches(ServerWebExchange exchange) {

        if (!isSearchRequest(exchange)) {
            return MatchResult.notMatch();
        }

        Mono<PortalSearchRequest> portalSearchRequest = extractBody(exchange);

        return portalSearchRequest
                .flatMap(request -> portalRepository.findPortalByIdAndStage(request.getPortalId(), gatewayService.getStage()))
                .map(Portal::getName)
                .flatMap(portalName ->
                        authorizationService
                                .getAccessInfoGroupForPortal(portalName)
                                .flatMap(accessInfoGroup -> this.matches(portalName, accessInfoGroup))
                );
    }

    private Mono<PortalSearchRequest> extractBody(ServerWebExchange exchange) {
        return DataBufferUtils.copyAsObject(exchange.getRequest().getBody(), PortalSearchRequest.class);
    }

    private boolean isSearchRequest(ServerWebExchange exchange) {
        String requestPath = exchange.getRequest().getPath().value();
        return requestPath.startsWith(searchProperties.getEndpoint());
    }

}


