package de.swm.lhm.geoportal.gateway.metadata.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "t_metadata")
public class Metadata {

    @Id
    @Column("id")
    @JsonIgnore
    Integer id;

    @Column("metadataid")
    String metadataUuid;

}
