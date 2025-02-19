package de.swm.lhm.geoportal.gateway.generic.model;


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
@Table(name = "t_module_generic")
public class GenericLayer {

    @Id
    @Column("id")
    String id;
    @Column("name")
    String name;
    @Column("json")
    String json;
    @Column("stageless_id")
    String stagelessId;
    @Column("stage")
    String stage;
    @Transient
    @Builder.Default
    AccessLevel accessLevel = AccessLevel.PUBLIC;
    @Transient
    @Builder.Default
    Boolean authLevelHigh = false;
}
