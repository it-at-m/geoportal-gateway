package de.swm.lhm.geoportal.gateway.geoservice.authorization;

import de.swm.lhm.geoportal.gateway.authorization.model.AuthorizationGroup;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import de.swm.lhm.geoportal.gateway.style_preview.model.StylePreviewGeoServiceLayer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class GeoServiceAuthorizationContext {

    public static final String MATCHRESULT_KEY = "gsAuthContext";

    @Builder.Default
    private boolean isSupportedRequest = true;

    /**
     * stylepreview attached to the geoservice request. Empty mono when none was attached
     */
    @Builder.Default
    private Flux<StylePreviewGeoServiceLayer> stylePreview = Flux.empty();

    @Builder.Default
    private Map<QualifiedLayerName, AuthorizationGroup> layerAuthorizationGroups = Collections.emptyMap();
}
