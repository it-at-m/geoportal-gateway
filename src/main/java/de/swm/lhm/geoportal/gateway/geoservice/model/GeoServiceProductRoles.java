package de.swm.lhm.geoportal.gateway.geoservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.swm.lhm.geoportal.gateway.authorization.model.AccessLevel;
import de.swm.lhm.geoportal.gateway.shared.model.Stage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "geoservice_product_roles_view")
public class GeoServiceProductRoles {

    @Column("resource_id")
    @JsonIgnore
    String resourceId;

    @Column("stage")
    @JsonIgnore
    Stage stage;

    @JsonIgnore
    @Column("access_level")
    AccessLevel accessLevel;

    @JsonIgnore
    @Column("auth_level_high")
    Boolean authLevelHigh;

    @Column("role_name")
    @JsonIgnore
    String roleName;
}
