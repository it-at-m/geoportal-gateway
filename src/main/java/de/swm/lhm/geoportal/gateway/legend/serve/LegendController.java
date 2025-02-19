package de.swm.lhm.geoportal.gateway.legend.serve;

import de.swm.lhm.geoportal.gateway.legend.LegendProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@Tag(name = "Legenden")
@RequiredArgsConstructor
@RestController
public class LegendController {
    private final LegendService legendService;
    private final LegendProperties legendProperties;
    @Operation(
            description = "Eine Legend-Datei anfragen",
            responses = {@ApiResponse(description = "Legend-Datei")}
    )
    @GetMapping("${geoportal.style.legend.endpoint}/**")
    public Mono<Resource> getLegend(ServerWebExchange exchange){
        String fullPath = exchange.getRequest().getURI().getPath();
        String pathWithinHandlerMapping = new AntPathMatcher().extractPathWithinPattern(legendProperties.getEndpoint() + "/**", fullPath);
        return legendService.getLegend(pathWithinHandlerMapping);
    }

}
