package de.swm.lhm.geoportal.gateway.search;

import de.swm.lhm.geoportal.gateway.search.model.masterportal.PortalSearchRequest;
import de.swm.lhm.geoportal.gateway.search.model.masterportal.PortalSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import de.swm.lhm.geoportal.gateway.shared.exceptions.DeserializationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

@Tag(name = "Suche")
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("${geoportal.gateway.search.endpoint}")
public class SearchController {
    private final SearchService searchService;

    @Operation(
            description = "Portalsuche"
    )
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "Ergebnisliste der Suche")
            })
    @PostMapping()
    Mono<PortalSearchResponse> portalSearch(@RequestBody PortalSearchRequest searchRequest) {
        return searchService.executePortalSearch(searchRequest);
    }

    @ResponseStatus(
            value = HttpStatus.NOT_FOUND,
            reason = "Ressource not found")
    @ExceptionHandler(NoSuchElementException.class)
    public void notFoundHandler(NoSuchElementException ex) {
        log.error(ex.getMessage(), ex);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DeserializationException.class)
    public void badRequestHandler(NoSuchElementException ex) {
        log.error(ex.getMessage(), ex);
    }
}
