package de.swm.lhm.geoportal.gateway.unit;


import de.swm.lhm.geoportal.gateway.unit.model.Unit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;


@Component
@RequiredArgsConstructor
public class UnitRepository {

    private final R2dbcEntityTemplate template;

    public Mono<Unit> findUnitById(Integer unitId) {
        return this.template.select(Unit.class)
                .matching(query(where("id").is(unitId)))
                .first();
    }

}

