package de.swm.lhm.geoportal.gateway.product;

import static de.swm.lhm.geoportal.gateway.util.AuthSqlUtils.getConcatenatedCriteriaFromAuthorizationGroup;
import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

import de.swm.lhm.geoportal.gateway.authorization.model.AuthorizationGroup;
import de.swm.lhm.geoportal.gateway.product.model.Product;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Component
@Slf4j
public class ProductRepository {

    private final R2dbcEntityTemplate template;

    public Flux<Product> findByStageAndAuthorizationGroup(Stage stage, AuthorizationGroup authorizationGroup) {

        OffsetDateTime now = OffsetDateTime.now(ZoneId.systemDefault());

        return this.template.select(Product.class)
                .matching(
                        query(
                                getConcatenatedCriteriaFromAuthorizationGroup(
                                        where("stage").is(stage)
                                                .and(validityStartIsNullOrLessThanOrEqualToNow(now))
                                                .and(validityEndIsNullOrGreaterThanOrEqualToNow(now)),
                                        authorizationGroup
                                )
                        )
                )
                .all();

    }

    public Mono<Product> findByNameAndStageAndAuthorizationGroup(String productName, Stage stage, AuthorizationGroup authorizationGroup) {

        OffsetDateTime now = OffsetDateTime.now(ZoneId.systemDefault());

        return this.template.select(Product.class)
                .matching(
                        query(
                                getConcatenatedCriteriaFromAuthorizationGroup(
                                        where("name").is(productName)
                                            .and("stage").is(stage)
                                            .and(validityStartIsNullOrLessThanOrEqualToNow(now))
                                            .and(validityEndIsNullOrGreaterThanOrEqualToNow(now)),
                                    authorizationGroup)
                        )
                )
                .first();

    }

    private Criteria validityStartIsNullOrLessThanOrEqualToNow(OffsetDateTime now){
        return where("validity_period_start").isNull()
                        .or("validity_period_start").lessThanOrEquals(now);
    }

    private Criteria validityEndIsNullOrGreaterThanOrEqualToNow(OffsetDateTime now){
        return where("validity_period_end").isNull()
                        .or("validity_period_end").greaterThanOrEquals(now);
    }

}

