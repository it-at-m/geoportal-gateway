package de.swm.lhm.geoportal.gateway.resource;


import de.swm.lhm.geoportal.gateway.resource.model.FileResource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;


@Component
@RequiredArgsConstructor
public class ResourceRepository {

    private final R2dbcEntityTemplate template;
    private final DatabaseClient databaseClient;


    public Flux<FileResource> findFileResourceByProductId(Integer productId) {

        return this.databaseClient.sql(
                        """
                        SELECT fileresource_id
                        FROM t_product_fileresource
                        WHERE product_id = :productId
                        """
                )
                .bind("productId", productId)
                .fetch()
                .all()
                .map(row -> row.get("fileresource_id"))
                .filter(Integer.class::isInstance)
                .map(Integer.class::cast)
                .distinct()
                .flatMap(this::findFileResourceById);
    }

    public Mono<FileResource> findFileResourceById(Integer fileResourceId) {
        return this.template.select(FileResource.class)
                .matching(query(where("id").is(fileResourceId))).first();
    }

}

