package de.swm.lhm.geoportal.gateway.geoservice;

import de.swm.lhm.geoportal.gateway.geoservice.model.PropertyField;
import de.swm.lhm.geoportal.gateway.geoservice.model.PropertyFieldEscaping;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class PropertyFieldRepository {
    private final DatabaseClient databaseClient;

    public Flux<PropertyField> findGeoServicePropertyFieldsByWorkspaceAndNameAndStage(String workspace, String name, Stage stage) {
        return this.databaseClient.sql("""
                                  with recursive hierarchy(parent, child) as (
                                      select parent, child 
                                          from t_containing_services
                                      union all
                                      select tcs.parent, tcs.child
                                          from t_containing_services tcs, hierarchy h
                                          where tcs.parent = h.child	
                                  )
                                  select pf.label,
                                        pf.escapeing, 
                                        pf.gfireferencefieldname, 
                                        pf.gfireferencetablename, 
                                        pf.gfireferenceschemaname, 
                                        pf.visible
                                  from (
                                      select distinct gs_id from (
                                          select h.child gs_id from hierarchy h 
                                          join t_geoservice gs on gs.id = h.parent
                                          where stage = :stage and name = :name and workspace = :workspace
                                          union
                                          select id 
                                          from t_geoservice
                                          where stage = :stage and name = :name and workspace = :workspace
                                      ) ids
                                  ) ids
                                  join t_geoservice gs on gs.id = ids.gs_id 
                                  join t_propertyquery pq on pq.geoservice_id = gs.id
                                  join t_propertyfield pf on pq.id = pf.propertyquery_id
                        """)
                .bind("stage", stage.name())
                .bind("name", name)
                .bind("workspace", workspace)
                .fetch()
                .all()
                .map(row -> new PropertyField(
                        (String) row.get("gfireferenceschemaname"),
                        (String) row.get("gfireferencetablename"),
                        (String) row.get("gfireferencefieldname"),
                        (String) row.get("label"),
                        (Boolean) row.get("visible"),
                        PropertyFieldEscaping.fromName((String) row.get("escapeing"))
                ));

    }


}
