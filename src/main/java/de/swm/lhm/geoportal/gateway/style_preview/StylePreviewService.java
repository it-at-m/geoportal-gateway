package de.swm.lhm.geoportal.gateway.style_preview;

import de.swm.lhm.geoportal.gateway.style_preview.model.StylePreviewGeoServiceLayer;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;



public interface StylePreviewService {

    Flux<StylePreviewGeoServiceLayer> getReferencedStylePreviews(ServerWebExchange exchange);

}
