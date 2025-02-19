package de.swm.lhm.geoportal.gateway.style_preview;

import de.swm.lhm.geoportal.gateway.style_preview.model.StylePreviewGeoServiceLayer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;

@Configuration
public class StylePreviewConfig {

    @Bean
    @ConditionalOnMissingBean(StylePreviewService.class)
    StylePreviewService getDummyStylePreviewService() {
        return new DummyStylePreviewService();
    }

    public static class DummyStylePreviewService implements StylePreviewService {

        @Override
        public Flux<StylePreviewGeoServiceLayer> getReferencedStylePreviews(ServerWebExchange exchange) {
            return Flux.just();
        }

    }

}