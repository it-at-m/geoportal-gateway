package de.swm.lhm.geoportal.gateway.style_preview.repository;

import de.swm.lhm.geoportal.gateway.shared.IsStageConfigurationCondition;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import de.swm.lhm.geoportal.gateway.style_preview.model.StylePreviewGeoServiceLayer;
import org.springframework.context.annotation.Conditional;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Conditional(IsStageConfigurationCondition.class)
@Repository
public interface StylePreviewGeoServiceLayerRepository extends R2dbcRepository<StylePreviewGeoServiceLayer, String> {

    @Query(
            """
            select
                tsp.id,
                tgs.name,
                tgs.workspace
            from (
                SELECT sp.id, sp.layer_id
                    FROM t_style_preview sp
                    WHERE sp.id = $1
                UNION
                    SELECT bl.style_preview_id as id, bl.layer_id
                    FROM t_style_preview_background_layer bl
                WHERE bl.style_preview_id = $1
            ) tsp
            join t_geoservice tgs on
                /* assume non-numeric characters in the layer_id string */
                case when tsp.layer_id ~ E'^\\\\d+$' /* the backslash in the regex is escaped twice, once for java, once for postgres */
                    then tsp.layer_id::integer = tgs.id
                    else false
                end
                AND tgs.stage = $2
           WHERE name IS NOT NULL 
             AND workspace IS NOT NULL
            """
    )
    Flux<StylePreviewGeoServiceLayer> findGeoServerLayersByStylePreviewIdAndStage(String id, String stage);


    default Flux<StylePreviewGeoServiceLayer> findGeoServerLayersByStylePreviewId(String id){
        return findGeoServerLayersByStylePreviewIdAndStage(id, Stage.CONFIGURATION.name());
    }
}
