package de.swm.lhm.geoportal.gateway.generic;


import de.swm.lhm.geoportal.gateway.generic.model.GenericLayer;
import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;


@RequiredArgsConstructor
@Component
public class GenericLayerRepository {

    private final R2dbcEntityTemplate template;
    private final DatabaseClient databaseClient;



    public Flux<GenericLayer> findSensorServiceByProductId(Integer productId, Stage stage) {

        return this.databaseClient.sql(
                        """
                        SELECT layer_id
                        FROM t_product_layer
                        WHERE product_id = :productId
                        AND layer_type = :layerType
                        """
                )
                .bind("productId", productId)
                .bind("layerType", ServiceType.GEN.toString())
                .fetch()
                .all()
                .map(row -> row.get("layer_id"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .distinct()
                .flatMap(stagelessId -> findSensorServiceByStagelessIdAndStage(stagelessId, stage));
    }

    public Mono<GenericLayer> findSensorServiceByStagelessIdAndStage(String stagelessId, Stage stage) {
        return this.template.select(GenericLayer.class)
                .matching(query(
                        where("stageless_id").is(stagelessId)
                                .and("stage").is(stage.name())
                )).first();
    }

}

