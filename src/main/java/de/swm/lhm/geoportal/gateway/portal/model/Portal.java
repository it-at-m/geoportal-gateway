package de.swm.lhm.geoportal.gateway.portal.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.swm.lhm.geoportal.gateway.authorization.model.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "t_portal")
public class Portal {

    @Id
    @Column("id")
    @JsonIgnore
    Integer id;

    @Column("name")
    String name;

    @Column("title")
    String title;

    @Transient
    String url;

    @Column("unit_id")
    @JsonIgnore
    Integer unitId;

    @Transient
    String unit;

    @Column("access_level")
    AccessLevel accessLevel;

    @Column("auth_level_high")
    Boolean authLevelHigh;

    @JsonIgnore
    @Column("search_index_geo_data")
    private String searchIndexGeoData;

    @JsonIgnore
    @Column("search_index_geo_service")
    private String searchIndexGeoService;

    @JsonIgnore
    @Column("search_index_meta_data")
    private String searchIndexMetaData;

}
