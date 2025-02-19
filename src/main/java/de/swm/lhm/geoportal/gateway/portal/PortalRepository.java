package de.swm.lhm.geoportal.gateway.portal;

import de.swm.lhm.geoportal.gateway.portal.model.Portal;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

@RequiredArgsConstructor
@Component
@Slf4j
public class PortalRepository {

    private final R2dbcEntityTemplate template;
    private final DatabaseClient databaseClient;


    public Flux<Portal> findPortalByProductId(Integer productId, Stage stage) {

        return this.databaseClient.sql(
                        """
                        SELECT portal_id
                        FROM t_product_portal
                        WHERE product_id = :productId
                        """
                )
                .bind("productId", productId)
                .fetch()
                .all()
                .map(row -> row.get("portal_id"))
                .filter(Integer.class::isInstance)
                .map(Integer.class::cast)
                .distinct()
                .flatMap(portalId -> findPortalByIdAndStage(portalId, stage));

    }

    public Mono<Portal> findPortalByIdAndStage(Integer portalId, Stage stage) {
        return this.template.select(Portal.class)
                .matching(query(
                        where("id").is(portalId)
                                .and("stage").is(stage)
                )).first();
    }
}

