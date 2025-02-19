package de.swm.lhm.geoportal.gateway.style_preview;

import de.swm.lhm.geoportal.gateway.shared.IsStageConfigurationCondition;
import de.swm.lhm.geoportal.gateway.style_preview.model.StylePreviewGeoServiceLayer;
import de.swm.lhm.geoportal.gateway.style_preview.repository.StylePreviewGeoServiceLayerRepository;
import de.swm.lhm.geoportal.gateway.style_preview.repository.StylePreviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static de.swm.lhm.geoportal.gateway.util.UuidUtils.isUuid;


@Conditional(IsStageConfigurationCondition.class)
@RequiredArgsConstructor
@Service
@Slf4j
public class ConfigStylePreviewService implements StylePreviewService {

    private static final String INDEX_HTML_FILENAME = "index.html";

    private final StylePreviewGeoServiceLayerRepository stylePreviewGeoServiceLayerRepository;
    private final StylePreviewRepository stylePreviewRepository;

    @Override
    public Flux<StylePreviewGeoServiceLayer> getReferencedStylePreviews(ServerWebExchange exchange) {

        return getSessionAttribute(exchange)
                .flatMapMany(stylePreviewGeoServiceLayerRepository::findGeoServerLayersByStylePreviewId);

    }

    public Mono<String> getSessionAttribute(ServerWebExchange exchange){

        return exchange.getSession()
                .mapNotNull(
                        webSession -> webSession.getAttribute(
                                StylePreviewProperties.STYLE_PREVIEW_SESSION_ATTRIBUTE_NAME
                        )
                )
                .filter(String.class::isInstance)
                .cast(String.class);

    }
    public Mono<Resource> setSessionAttributeIfIndexHtml(
            Resource resource,
            ServerWebExchange exchange,
            String stylePreviewId
    ) {
        String fileName = resource.getFilename();

        if (StringUtils.isBlank(fileName) || !fileName.equalsIgnoreCase(INDEX_HTML_FILENAME))
            return Mono.just(resource);

        return setSessionAttribute(exchange, stylePreviewId)
                .map(done -> resource);
    }

    public Mono<Boolean> setSessionAttribute(
            ServerWebExchange exchange,
            String stylePreviewId
    ){

        return exchange.getSession()
                .map(
                        webSession -> {

                            log.debug(
                                    "Setting session attribute {} for style preview {}",
                                    StylePreviewProperties.STYLE_PREVIEW_SESSION_ATTRIBUTE_NAME,
                                    stylePreviewId
                            );

                            webSession.getAttributes()
                                    .put(
                                            StylePreviewProperties.STYLE_PREVIEW_SESSION_ATTRIBUTE_NAME,
                                            stylePreviewId
                                    );
                            return true;
                        }
                )
                .or(Mono.just(false));

    }

    public Mono<Boolean> isValidStylePreview(String stylePreviewId) {

        if (StringUtils.isBlank(stylePreviewId) || !isUuid(stylePreviewId))
            return Mono.just(false);
        return stylePreviewRepository.existsById(stylePreviewId);

    }
}
