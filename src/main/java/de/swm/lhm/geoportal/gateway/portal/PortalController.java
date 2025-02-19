package de.swm.lhm.geoportal.gateway.portal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static de.swm.lhm.geoportal.gateway.util.FileServeUtils.createRedirectToRootPath;
import static de.swm.lhm.geoportal.gateway.util.FileServeUtils.getFileAsResourceEnsuringAbsoluteRootPath;


@Tag(name = "Portale")
@RestController
@Slf4j
@RequiredArgsConstructor
public class PortalController {

    private final PortalProperties portalProperties;

    @Operation(
            description = "Auf Portal weiterleiten"
    )
    @GetMapping("${geoportal.gateway.portal.endpoint}/{name}")
    public Mono<Void> redirectToIndex(
            ServerHttpRequest request,
            ServerHttpResponse response,
            @Parameter(description = "Name von Portal auf welches weitergeleitet werden soll") @PathVariable String name
    ) {
        return createRedirectToRootPath(request, response, name);
    }

    @Operation(
            description = "Portal anzeigen"
    )
    @GetMapping("${geoportal.gateway.portal.endpoint}/{name}/")
    public Mono<Resource> serveIndex(
            @Parameter(description = "Name von Portal welches angezeigt werden soll") @PathVariable String name
    ) {

        return getFileAsResourceEnsuringAbsoluteRootPath(
                portalProperties.getPath(),
                name, "index.html"
        );

    }

    @Operation(
            description = "Portaldatei anfragen"
    )
    @GetMapping("${geoportal.gateway.portal.endpoint}/**")
    public Mono<Resource> servePortalFile(
            ServerHttpRequest request
    ) {

        return getFileAsResourceEnsuringAbsoluteRootPath(
                portalProperties.getPath(),
                request.getPath()
                        .value()
                        .substring(portalProperties.getEndpointWithSlashesLength())
        );

    }

}
