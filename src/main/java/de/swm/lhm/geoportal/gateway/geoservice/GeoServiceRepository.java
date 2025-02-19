package de.swm.lhm.geoportal.gateway.geoservice;


import de.swm.lhm.geoportal.gateway.authorization.model.AuthorizationGroup;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import de.swm.lhm.geoportal.gateway.geoservice.model.GeoService;
import de.swm.lhm.geoportal.gateway.geoservice.model.GeoServiceProductRoles;
import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static de.swm.lhm.geoportal.gateway.util.AuthSqlUtils.getConcatenatedCriteriaFromAuthorizationGroup;
import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;


@RequiredArgsConstructor
@Component
public class GeoServiceRepository {

    private final R2dbcEntityTemplate template;
    private final DatabaseClient databaseClient;


    public Flux<GeoService> findGeoServiceByProductId(Integer productId, Stage stage) {

        return this.databaseClient.sql(
                        """
                                SELECT layer_id
                                FROM t_product_layer
                                WHERE product_id = :productId
                                AND layer_type IN (:serviceTypes)
                                """
                )
                .bind("productId", productId)
                .bind("serviceTypes", ServiceType.getAllGeoServerServices().stream().map(String::valueOf).toList())
                .fetch()
                .all()
                .map(row -> row.get("layer_id"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(value -> value.split(":"))
                .distinct()
                .flatMap(value -> findGeoServiceByWorkspaceAndNameAndStage(value[0], value[1], stage))
                .delayUntil(this::addServiceTypesByGeoService);
    }

    public Mono<GeoService> findGeoServiceByWorkspaceAndNameAndStage(String workspace, String name, Stage stage) {
        return this.template.select(GeoService.class)
                .matching(query(
                        where("workspace").is(workspace)
                                .and("name").is(name)
                                .and("stage").is(stage.name())
                )).first();
    }

    public Mono<GeoService> addServiceTypesByGeoService(GeoService geoService) {
        return this.databaseClient.sql(
                        """
                                SELECT DISTINCT name 
                                FROM t_servicetype
                                WHERE geoservice_id = :geoserviceId
                                """
                )
                .bind("geoserviceId", geoService.getId())
                .fetch()
                .all()
                .map(row -> row.get("name"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(ServiceType::valueOf)
                .collectList()
                .map(geoServiceTypes -> {
                    geoService.setServiceTypes(geoServiceTypes);
                    return geoService;
                });
    }

    public Flux<String> findNonHiddenGeoServiceLayers(Stage stage, AuthorizationGroup authorizationGroup) {

        return this.template.select(GeoServiceProductRoles.class)
                .matching(
                        query(
                                getConcatenatedCriteriaFromAuthorizationGroup(
                                        where("stage").is(stage),
                                        authorizationGroup
                                )
                        )
                )
                .all()
                .map(GeoServiceProductRoles::getResourceId);
    }

    public Mono<Boolean> layerHasWfsTEnabled(Stage stage, QualifiedLayerName qualifiedLayerName) {
        return this.databaseClient.sql(
                        """
                                select exists(
                                    select tw.portal_id
                                    from t_wfst tw
                                    join t_portal tp on tw.portal_id = tp.id
                                    where tw.wfst_layer = :layer
                                        and tp.stage = :stage
                                        and tp.wfst_editable = true
                                ) as has_wfst
                                """
                )
                .bind("layer", qualifiedLayerName.toString())
                .bind("stage", stage.name())
                .fetch()
                .one()
                .map(row -> row.get("has_wfst"))
                .map(Boolean.class::cast);
    }

    public Flux<QualifiedLayerName> findContainedQualifiedLayerNames(Stage stage, QualifiedLayerName parentQualifiedLayerName) {
        return databaseClient.sql(
                        """
                                select tg_child.name, tg_child.workspace 
                                from geoservice_and_ancestor_view a
                                join t_geoservice tg_parent on a.ancestor_or_self=tg_parent.id
                                join t_geoservice tg_child on a.self=tg_child.id
                                where tg_parent.workspace=:workspace 
                                    and tg_parent.name=:name 
                                    and tg_parent.stage=:stage
                                    and tg_child.stage=:stage
                                """
                )
                .bind("name", parentQualifiedLayerName.layerName())
                .bind("workspace", parentQualifiedLayerName.workspaceName())
                .bind("stage", stage.name())
                .fetch()
                .all()
                .map(row -> new QualifiedLayerName(((String) row.get("workspace")), (String) row.get("name")));
    }
}

