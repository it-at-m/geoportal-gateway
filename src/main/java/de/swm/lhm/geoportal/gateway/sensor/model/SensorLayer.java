package de.swm.lhm.geoportal.gateway.sensor.model;


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
@Table(name = "t_plugin_sta_sensor")
public class SensorLayer {

    @Id
    @Column("id")
    String id;
    @Column("name")
    String name;
    @Column("url")
    String url;
    @Column("stageless_id")
    String stagelessId;
    @Transient
    @Builder.Default
    AccessLevel accessLevel = AccessLevel.PUBLIC;
    @Transient
    @Builder.Default
    Boolean authLevelHigh = false;
}
