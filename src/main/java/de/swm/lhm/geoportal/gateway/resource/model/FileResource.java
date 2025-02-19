package de.swm.lhm.geoportal.gateway.resource.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import de.swm.lhm.geoportal.gateway.authorization.model.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "t_file_resource")
public class FileResource {

    @Id
    @Column("id")
    @JsonIgnore
    Integer id;

    @Column("name")
    String name;

    @Column("unit")
    String unit;

    @Transient
    String url;

    @Transient
    @Builder.Default
    AccessLevel accessLevel = AccessLevel.PUBLIC;

    @Transient
    @Builder.Default
    Boolean authLevelHigh = false;

}
