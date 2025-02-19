package de.swm.lhm.geoportal.gateway.style_preview;

import de.swm.lhm.geoportal.gateway.shared.IsStageConfigurationCondition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.io.Resource;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static de.swm.lhm.geoportal.gateway.util.FileServeUtils.createRedirectToRootPath;
import static de.swm.lhm.geoportal.gateway.util.FileServeUtils.getFileAsResourceEnsuringAbsoluteRootPath;
import static de.swm.lhm.geoportal.gateway.util.FileServeUtils.throwNotFound;

@Tag(name = "Style Preview")
@Conditional(IsStageConfigurationCondition.class)
@RestController
@Slf4j
@RequiredArgsConstructor
public class StylePreviewController {

    private final StylePreviewProperties stylePreviewProperties;
    private final ConfigStylePreviewService stylePreviewService;

    @Operation(
            description = "Weiterleiten"
    )
    @GetMapping("${geoportal.gateway.style-preview.endpoint}/{uuid}")
    public Mono<Void> redirectToIndex(
            ServerHttpRequest request,
            ServerHttpResponse response,
            @Parameter(description = "UUID auf welche weitergeleitet werden soll") @PathVariable String uuid
    ) {
        return stylePreviewService.isValidStylePreview(uuid)
                .flatMap(exists -> {
                    if (Boolean.FALSE.equals(exists))
                        throwNotFound("style preview with id '" + uuid + "' not found");
                    return createRedirectToRootPath(request, response, uuid);
                });
    }

    @Operation(
            description = "Style anfragen"
    )
    @GetMapping("${geoportal.gateway.style-preview.endpoint}/{uuid}/")
    public Mono<Resource> serveIndex(
            ServerWebExchange exchange,
            @Parameter(description = "Style UUID welche zurückgegeben werden soll") @PathVariable String uuid
    ) {
        return serveStylePreviewFile(
                "index.html",
                uuid,
                exchange
        );
    }

    @Operation(
            description = "Style Vorschau anfragen"
    )
    @GetMapping("${geoportal.gateway.style-preview.endpoint}/{uuid}/**")
    public Mono<Resource> serveStylePreviewFile(
            ServerWebExchange exchange,
            @Parameter(description = "Style UUID für Vorschau") @PathVariable String uuid
    ) {

        String fileName = exchange.getRequest().getPath()
                .value()
                .substring(stylePreviewProperties.getEndpointWithSlashesLength() + uuid.length() + 1);

        log.debug("serving file: {}", fileName);

        return serveStylePreviewFile(
                fileName,
                uuid,
                exchange
        );

    }

    private Mono<Resource> serveStylePreviewFile(
            String filePath,
            String stylePreviewId,
            ServerWebExchange exchange
    ) {

        return stylePreviewService.isValidStylePreview(stylePreviewId)
                .flatMap(
                        exists -> {
                            if (Boolean.FALSE.equals(exists))
                                throwNotFound("style preview with id '" + stylePreviewId + "' not found");
                            return getFileAsResourceEnsuringAbsoluteRootPath(
                                    stylePreviewProperties.getPath(),
                                    stylePreviewId,
                                    filePath
                            );
                        }
                )
                .delayUntil(
                        resource -> stylePreviewService.setSessionAttributeIfIndexHtml(
                                resource,
                                exchange,
                                stylePreviewId
                        )
                );


    }


}
