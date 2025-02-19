package de.swm.lhm.geoportal.gateway.metadata;


import de.swm.lhm.geoportal.gateway.metadata.model.Metadata;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;


@Component
@RequiredArgsConstructor
public class MetadataRepository {

    private final R2dbcEntityTemplate template;

    public Mono<Metadata> findMetadataById(Integer id) {
        return this.template.select(Metadata.class)
                .matching(query(where("id").is(id))).first();
    }

}

