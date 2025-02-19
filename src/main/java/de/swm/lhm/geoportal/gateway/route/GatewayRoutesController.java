package de.swm.lhm.geoportal.gateway.route;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "Gateway Routes")
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("${geoportal.gateway.routes.refresh-endpoint}")
public class GatewayRoutesController {

    private final GatewayRoutesRefresher gatewayRoutesRefresher;
    public static final String RESPONSE_MESSAGE = "routes reloaded successfully";


    @Operation(
            description = "Routing aktualisieren",
            responses = {@ApiResponse(description = "Erfolgsmeldung")}
    )
    @PostMapping
    public Mono<String> refreshRoutes() {

        gatewayRoutesRefresher.refreshRoutes();

        return Mono.just(RESPONSE_MESSAGE);

    }

}
