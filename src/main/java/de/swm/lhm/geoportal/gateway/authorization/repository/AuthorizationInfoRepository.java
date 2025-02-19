package de.swm.lhm.geoportal.gateway.authorization.repository;

import de.swm.lhm.geoportal.gateway.authorization.model.AuthorizationInfo;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;


@Repository
public interface AuthorizationInfoRepository extends R2dbcRepository<AuthorizationInfo, Integer> {

    @Query(
            """
            SELECT DISTINCT
                role_name,
                auth_level_high,
                access_level,
                resource_id
            FROM portal_product_roles_view
            WHERE resource_id = $1 
              AND stage = $2
            """
    )
    Flux<AuthorizationInfo> findAuthorizationInfoByResourceIdAndStage(String name, Stage stage);

    @Query(
            // this intentionally compares case-insensitve.
            // Geoserver also is case-insensitive regarding layernames.
            """
                    select distinct 
                       role_name, 
                       auth_level_high, 
                       access_level,
                       resource_id 
                    from geoservice_product_roles_view gpr 
                    where gpr.resource_id ilike $1 and gpr.stage = $2
            """
    )
    Flux<AuthorizationInfo> findAuthorizationInfoByGeoServiceLayerAndStage(String geoServiceLayer, Stage stage);


    @Query("""
                    select distinct
                        role_name,
                        auth_level_high,
                        access_level,
                        resource_id
                    from resource_product_roles_view rprv
                    where rprv.unit = $1 and rprv.name = $2 and (rprv.stage = $3 or rprv.stage is null)
            """)
    Flux<AuthorizationInfo> findAuthorizationInfoForFileResource(String unit, String name, Stage stage);
}
