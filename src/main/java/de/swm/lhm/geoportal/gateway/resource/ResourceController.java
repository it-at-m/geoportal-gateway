package de.swm.lhm.geoportal.gateway.resource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.file.Paths;

@Tag(name = "Ressourcen")
@RequiredArgsConstructor
@RestController
@ConditionalOnProperty(prefix = "geoportal.resource", name = "enable-webserver", havingValue = "false")
public class ResourceController {
    private final ResourceProperties resourceProperties;
    private final ResourceService resourceService;

    @Operation(
            description = "Eine Ressourcen-Datei anfragen",
            responses = {@ApiResponse(description = "Ressourcen-Datei")}
    )
    @GetMapping("${geoportal.resource.endpoint}/**")
    public Mono<ResponseEntity<Resource>> getResource(ServerWebExchange exchange) {
        String fullPath = exchange.getRequest().getURI().getPath();
        String pathWithinHandlerMapping = new AntPathMatcher().extractPathWithinPattern(resourceProperties.getEndpoint() + "/**", fullPath);
        String fileName = Paths.get(pathWithinHandlerMapping).getFileName().toString();
        return resourceService.getResource(pathWithinHandlerMapping)
                .map(resource ->
                        ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName)
                                .body(resource)
                );
    }
}
