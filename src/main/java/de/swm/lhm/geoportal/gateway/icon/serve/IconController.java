package de.swm.lhm.geoportal.gateway.icon.serve;

import de.swm.lhm.geoportal.gateway.icon.IconProperties;
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

@Tag(name = "Icons")
@RequiredArgsConstructor
@RestController
public class IconController {
    private final IconService iconService;
    private final IconProperties iconProperties;

    @Operation(
            description = "Eine Icon-Datei anfragen",
            responses = {@ApiResponse(description = "Icon-Datei")}
    )
    @GetMapping("${geoportal.style.icon.endpoint}/**")
    public Mono<Resource> getIcon(ServerWebExchange exchange){
        String fullPath = exchange.getRequest().getURI().getPath();
        String pathWithinHandlerMapping = new AntPathMatcher().extractPathWithinPattern(iconProperties.getEndpoint() + "/**", fullPath);
        return iconService.getIcon(pathWithinHandlerMapping);
    }

}
