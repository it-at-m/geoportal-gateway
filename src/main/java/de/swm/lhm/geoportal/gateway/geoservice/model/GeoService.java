package de.swm.lhm.geoportal.gateway.geoservice.model;


import de.swm.lhm.geoportal.gateway.authorization.model.AccessLevel;
import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import de.swm.lhm.geoportal.gateway.shared.model.ServiceType;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "t_geoservice")
public class GeoService {

    @Id
    @Column("id")
    Integer id;
    @Column("name")
    String name;
    @Column("workspace")
    String workspace;
    @Column("stage")
    Stage stage;
    @Column("access_level")
    AccessLevel accessLevel;
    @Column("auth_level_high")
    Boolean authLevelHigh;
    @Transient
    List<ServiceType> serviceTypes;

    public QualifiedLayerName getQualifiedLayerName() {
        return new QualifiedLayerName(workspace, name);
    }
}
